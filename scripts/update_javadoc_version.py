#!/usr/bin/env python3
"""Update or validate JavaDoc @version and @since tags across Java source files.

Two modes:

* Tree-wide (legacy): ``--check --expected V`` or ``--apply --expected V``.
  Walks every ``*.java`` under ``src/`` and ``test/``. Retained for backward
  compatibility and the deprecated ``update-baseline-version.sh`` flow.

* PR-diff scoped: ``--check --changed-only --expected V [--base-ref REF]``.
  Validates only files that the current branch modified or added relative to
  ``REF`` (default ``origin/main``). This matches PR review semantics and is
  the mode the CI workflow uses.

Diff scope semantics:

* Modified files (``--diff-filter=M``): ``@version`` must equal milestone;
  ``@since`` must be byte-equal to its value on the base ref (immutable).
* Added/renamed/copied files (``--diff-filter=ARC``): both ``@version`` and
  ``@since`` must equal milestone. Renames are intentionally treated as
  additions, which forces ``@since`` to reset.
* Deleted files: not checked.
* Empty diff: exit 0 with an INFO message.

We use triple-dot ``base...HEAD`` to compare against the merge-base, matching
GitHub's PR diff behavior.
"""

import argparse
import re
import subprocess
import sys
from pathlib import Path
from typing import List, Optional, Tuple


def find_java_files(scope: str) -> List[Path]:
    """Find all Java files in the specified scope."""
    paths = []
    if scope in ('all', 'src'):
        paths.append(Path('src'))
    if scope in ('all', 'test'):
        paths.append(Path('test'))

    java_files = []
    for path in paths:
        if path.exists():
            java_files.extend(path.rglob('*.java'))

    return sorted(java_files)


def update_file(file_path: Path, version: str) -> None:
    """Update @version and @since tags in a single file."""
    content = file_path.read_text()
    lines = content.split('\n')
    result = []
    i = 0
    first_class_found = False

    while i < len(lines):
        line = lines[i]

        # Detect JavaDoc block start
        if re.match(r'^\s*/\*\*', line):
            # Single-line javadoc /** ... */ — preserve as-is. These are
            # field/method/enum-constant docs; injecting class-level tags
            # would corrupt the file. If the line happens to carry
            # @version/@since (rare), rewrite them in place.
            if '*/' in line:
                if '@version' in line:
                    line = re.sub(r'(@version\s+)\S+', rf'\g<1>{version}', line)
                if '@since' in line:
                    line = re.sub(r'(@since\s+)\S+', rf'\g<1>{version}', line)
                result.append(line)
                i += 1
                continue

            javadoc_lines = [line]
            i += 1
            has_version = False
            has_since = False

            # Collect all lines of the JavaDoc block
            while i < len(lines):
                line = lines[i]

                # Update @version tag
                if '@version' in line:
                    has_version = True
                    line = re.sub(r'(@version\s+)\S+', rf'\g<1>{version}', line)

                # Update @since tag
                if '@since' in line:
                    has_since = True
                    line = re.sub(r'(@since\s+)\S+', rf'\g<1>{version}', line)

                javadoc_lines.append(line)

                # End of JavaDoc
                if '*/' in line:
                    # If missing tags, add them before closing
                    if not has_version or not has_since:
                        indent_match = re.match(r'^(\s*)', line)
                        indent = indent_match.group(1) if indent_match else ''
                        closing_line = javadoc_lines.pop()

                        if not has_version:
                            javadoc_lines.append(f'{indent} * @version {version}')
                        if not has_since:
                            javadoc_lines.append(f'{indent} * @since {version}')

                        javadoc_lines.append(closing_line)

                    result.extend(javadoc_lines)
                    i += 1
                    break

                i += 1
            continue

        # Detect ONLY top-level class/interface/enum (not nested, not methods)
        # Must be: (modifiers)? (class|interface|enum) IDENTIFIER (whitespace|<|{|extends|implements)
        class_match = re.match(
            r'^\s*(public|private|protected)?\s*(abstract|final|static)?\s*'
            r'(class|interface|enum)\s+([A-Za-z_]\w*)(\s*[<{\s]|$)',
            line
        )

        if class_match and not first_class_found:
            first_class_found = True

            # Look back to see if there's a JavaDoc block within last few lines
            has_javadoc = False
            lookback = min(15, len(result))
            for j in range(len(result) - lookback, len(result)):
                if '*/' in result[j]:
                    has_javadoc = True
                    break

            # If no JavaDoc, add minimal one
            if not has_javadoc:
                classname = class_match.group(4)
                indent_match = re.match(r'^(\s*)', line)
                indent = indent_match.group(1) if indent_match else ''

                result.append(f'{indent}/**')
                result.append(f'{indent} * {classname}.')
                result.append(f'{indent} *')
                result.append(f'{indent} * @version {version}')
                result.append(f'{indent} * @since {version}')
                result.append(f'{indent} */')

        result.append(line)
        i += 1

    file_path.write_text('\n'.join(result))


def check_file(file_path: Path, expected: str) -> Tuple[bool, List[str]]:
    """Check if file has correct version tags."""
    violations = []
    content = file_path.read_text()

    version_match = re.search(r'@version\s+(\S+)', content)
    if not version_match:
        violations.append(f"MISSING @version: {file_path}")
    elif version_match.group(1) != expected:
        violations.append(f"WRONG @version ({version_match.group(1)} != {expected}): {file_path}")

    since_match = re.search(r'@since\s+(\S+)', content)
    if not since_match:
        violations.append(f"MISSING @since: {file_path}")
    elif since_match.group(1) != expected:
        violations.append(f"WRONG @since ({since_match.group(1)} != {expected}): {file_path}")

    return len(violations) == 0, violations


def _git(*args: str) -> str:
    """Run git with the given args; return stripped stdout."""
    return subprocess.check_output(["git", *args], text=True).rstrip("\n")


def changed_java_files(base_ref: str) -> Tuple[List[Path], List[Path]]:
    """Return ``(modified_files, added_files)`` under src/ or test/, *.java only.

    Uses ``base_ref...HEAD`` (triple-dot) so we diff against the merge-base —
    matching PR diff semantics. Renames/copies are reported as added
    (decision 1b in the plan); deletions are excluded.
    """
    diff_range = f"{base_ref}...HEAD"
    modified_raw = _git(
        "diff", "--name-only", "--diff-filter=M", diff_range,
        "--", "src/", "test/",
    )
    added_raw = _git(
        "diff", "--name-only", "--diff-filter=ARC", diff_range,
        "--", "src/", "test/",
    )

    def _parse(raw: str) -> List[Path]:
        return [Path(line) for line in raw.splitlines()
                if line and line.endswith(".java")]

    return _parse(modified_raw), _parse(added_raw)


def since_on_base(file_path: Path, base_ref: str) -> Optional[str]:
    """Read ``@since`` from ``file_path`` as it exists on ``base_ref``.

    Returns the value string, or ``None`` if the tag is absent on the base ref.
    Returns ``None`` if the file cannot be read on the base ref (caller decides
    how to handle that case).
    """
    try:
        content = subprocess.check_output(
            ["git", "show", f"{base_ref}:{file_path}"],
            text=True,
            stderr=subprocess.DEVNULL,
        )
    except subprocess.CalledProcessError:
        return None
    match = re.search(r"@since\s+(\S+)", content)
    return match.group(1) if match else None


def _read_tag(content: str, tag: str) -> Optional[str]:
    """Return the first value of ``@tag`` in ``content`` or None."""
    match = re.search(rf"@{tag}\s+(\S+)", content)
    return match.group(1) if match else None


def check_changed_only(modified: List[Path], added: List[Path],
                       milestone: str, base_ref: str,
                       allow_since_mutation: bool = False
                       ) -> Tuple[int, List[str], List[str]]:
    """Validate the PR-scoped diff against the canonical Javadoc rules.

    Returns ``(violation_count, error_messages, warning_messages)``. The
    count is the number of distinct files with at least one **blocking**
    problem; warnings (currently only @since-immutability violations
    when ``allow_since_mutation`` is set) do not contribute to the count.
    """
    violations: List[str] = []
    warnings: List[str] = []
    bad_files: set = set()

    def _flag(file_path: Path, message: str) -> None:
        violations.append(message)
        bad_files.add(file_path)

    for file_path in modified:
        if not file_path.exists():
            # Filtered earlier, but be defensive — re-classify as "could not read".
            _flag(file_path, f"MISSING file on disk (modified diff entry): {file_path}")
            continue
        content = file_path.read_text()

        version = _read_tag(content, "version")
        if version is None:
            _flag(file_path, f"MISSING @version: {file_path}")
        elif version != milestone:
            _flag(file_path,
                  f"WRONG @version ({version} != {milestone}): {file_path} "
                  f"(modified files must bump @version to current milestone)")

        current_since = _read_tag(content, "since")
        if current_since is None:
            _flag(file_path,
                  f"MISSING @since: {file_path} "
                  f"(add @since matching when the file was introduced)")
            continue

        base_since = since_on_base(file_path, base_ref)
        if base_since is None:
            # File existed on base ref but had no @since (or could not be read).
            # Treat as a fix-forward: require @since == milestone.
            if current_since != milestone:
                _flag(file_path,
                      f"MISSING @since on base ref; current @since "
                      f"({current_since}) must equal milestone ({milestone}) "
                      f"as a fix-forward: {file_path}")
        elif current_since != base_since:
            immutability_msg = (
                f"@since IS IMMUTABLE ({current_since} != {base_since} on "
                f"{base_ref}): {file_path} "
                f"(do not edit @since on existing files)"
            )
            if allow_since_mutation:
                warnings.append(immutability_msg)
            else:
                _flag(file_path, immutability_msg)

    for file_path in added:
        if not file_path.exists():
            _flag(file_path, f"MISSING file on disk (added diff entry): {file_path}")
            continue
        content = file_path.read_text()

        version = _read_tag(content, "version")
        if version is None:
            _flag(file_path, f"MISSING @version: {file_path}")
        elif version != milestone:
            _flag(file_path,
                  f"WRONG @version ({version} != {milestone}): {file_path} "
                  f"(new files must set @version to current milestone)")

        current_since = _read_tag(content, "since")
        if current_since is None:
            _flag(file_path, f"MISSING @since: {file_path}")
        elif current_since != milestone:
            _flag(file_path,
                  f"WRONG @since ({current_since} != {milestone}): {file_path} "
                  f"(new files must set @since to current milestone)")

    return len(bad_files), violations, warnings


def _ensure_base_ref(base_ref: str) -> Optional[str]:
    """Verify ``base_ref`` is resolvable locally; return error string or None."""
    try:
        subprocess.check_output(
            ["git", "rev-parse", "--verify", base_ref],
            text=True,
            stderr=subprocess.DEVNULL,
        )
    except subprocess.CalledProcessError:
        return (
            f"ERROR: base ref '{base_ref}' is not resolvable locally. "
            f"Run `git fetch origin main` (or pass --base-ref REF) and retry."
        )
    return None


def main():
    parser = argparse.ArgumentParser(description='Update or validate JavaDoc version tags')
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--expected', default='')
    parser.add_argument('--scope', default='all', choices=['all', 'src', 'test'])
    parser.add_argument('--changed-only', action='store_true',
                        help='Validate only files modified or added vs --base-ref '
                             '(requires --check).')
    parser.add_argument('--base-ref', default='origin/main',
                        help='Git ref to diff against in --changed-only mode '
                             '(default: origin/main).')
    parser.add_argument('--allow-since-mutation', action='store_true',
                        help='Downgrade @since-immutability violations to '
                             'warnings (PR still passes). One-shot escape '
                             'hatch for repo-wide baseline resets; do NOT '
                             'use for normal development.')

    args = parser.parse_args()

    if not args.apply and not args.check:
        print('ERROR: Must specify --apply or --check', file=sys.stderr)
        return 1

    if args.apply and args.changed_only:
        print('ERROR: --apply --changed-only is not supported. '
              'Apply mode is a maintenance tool and must run tree-wide.',
              file=sys.stderr)
        return 1

    if args.check and not args.expected:
        print('ERROR: --check requires --expected VERSION', file=sys.stderr)
        return 1

    if args.apply and not args.expected:
        args.expected = 'v0.1.0'

    # PR-diff-scoped mode (--changed-only) ----------------------------------
    if args.check and args.changed_only:
        base_ref_error = _ensure_base_ref(args.base_ref)
        if base_ref_error is not None:
            print(base_ref_error, file=sys.stderr)
            return 1

        try:
            modified, added = changed_java_files(args.base_ref)
        except subprocess.CalledProcessError as exc:
            print(f'ERROR: git diff against {args.base_ref} failed: {exc}',
                  file=sys.stderr)
            return 1

        total_changed = len(modified) + len(added)
        if total_changed == 0:
            print(f'INFO: No Java files modified or added vs {args.base_ref}; '
                  f'skipping validation.')
            return 0

        print(f'INFO: Validating PR-scoped diff vs {args.base_ref}: '
              f'{len(modified)} modified, {len(added)} added '
              f'(milestone: {args.expected})')

        bad_count, messages, warnings = check_changed_only(
            modified, added, args.expected, args.base_ref,
            allow_since_mutation=args.allow_since_mutation,
        )
        for message in messages:
            print(f'  ✗ {message}')
        for warning in warnings:
            print(f'  ⚠ (downgraded by --allow-since-mutation) {warning}')

        if bad_count > 0:
            print(f'\nERROR: Found {bad_count} file(s) with violations',
                  file=sys.stderr)
            return 1

        if warnings:
            print(f'INFO: {len(warnings)} @since mutation(s) accepted under '
                  f'--allow-since-mutation; all blocking checks passed for '
                  f'{args.expected}')
        else:
            print(f'INFO: All {total_changed} changed Java file(s) have correct '
                  f'@version and @since for {args.expected}')
        return 0

    # Tree-wide (legacy) mode ----------------------------------------------
    java_files = find_java_files(args.scope)
    if not java_files:
        print(f'WARNING: No Java files found in scope: {args.scope}', file=sys.stderr)
        return 0

    print(f'INFO: Found {len(java_files)} Java files in scope: {args.scope}')

    if args.apply:
        print(f'INFO: Applying baseline version: {args.expected}')
        for file_path in java_files:
            update_file(file_path, args.expected)
        print(f'INFO: Successfully updated {len(java_files)} files')

    elif args.check:
        print(f'INFO: Checking for version: {args.expected}')
        total_violations = 0
        for file_path in java_files:
            is_valid, violations = check_file(file_path, args.expected)
            if not is_valid:
                for violation in violations:
                    print(f'  ✗ {violation}')
                total_violations += 1

        if total_violations > 0:
            print(f'\nERROR: Found {total_violations} files with violations', file=sys.stderr)
            return 1
        else:
            print(f'INFO: All {len(java_files)} files have correct tags: {args.expected}')

    return 0


if __name__ == '__main__':
    sys.exit(main())
