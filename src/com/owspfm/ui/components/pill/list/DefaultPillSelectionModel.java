package com.owspfm.ui.components.pill.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default {@link PillSelectionModel} backed by a {@link LinkedHashSet}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class DefaultPillSelectionModel<T> implements PillSelectionModel<T> {

  private final Set<T> mySelected = new LinkedHashSet<>();
  private final List<PillSelectionListener<T>> myListeners = new ArrayList<>();
