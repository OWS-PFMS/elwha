# Card-list demo

The generic list primitive (`ElwhaItemList<T>`) lives in [`../../list/`](../../list/) as of [epic #67](https://github.com/OWS-PFMS/elwha/issues/67). This package retains only the `ElwhaCardListDemo` launcher (preserved as a known main-class path for smoke-testing the list with `ElwhaCard` items); the legacy `ElwhaCardList<T>` class and its 14 parallel `Card*` support classes were deleted in [#70](https://github.com/OWS-PFMS/elwha/issues/70).

```bash
mvn -q compile exec:java \
  -Dexec.mainClass="com.owspfm.elwha.card.list.ElwhaCardListDemo"
```

See [`../../list/README.md`](../../list/README.md) for the full `ElwhaItemList<T>` API.
