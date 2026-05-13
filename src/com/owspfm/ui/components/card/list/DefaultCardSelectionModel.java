package com.owspfm.ui.components.card.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default {@link CardSelectionModel} backed by a {@link LinkedHashSet}.
 *
 * @param <T> the item type
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public class DefaultCardSelectionModel<T> implements CardSelectionModel<T> {

  private final Set<T> mySelected = new LinkedHashSet<>();
  private final List<CardSelectionListener<T>> myListeners = new ArrayList<>();
