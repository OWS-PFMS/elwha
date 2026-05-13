package com.owspfm.ui.components.card.playground;

import java.time.LocalDate;

/**
 * Lightweight sample record rendered by the FlatCardList showcase.
 *
 * <p>Holds a title, subtitle, date, and priority — enough for the showcase to demonstrate sort (by
 * title / date / priority), filter (text-based), and reorder.
 *
 * @param title the item title
 * @param subtitle the item subtitle
 * @param date the item date
 * @param priority the item priority (1 = lowest)
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public record DemoItem(String title, String subtitle, LocalDate date, int priority) {}
