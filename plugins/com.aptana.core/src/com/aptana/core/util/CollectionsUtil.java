/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aptana.core.IFilter;
import com.aptana.core.IMap;

/**
 * Utility functions for set-like operations on collections
 */
public class CollectionsUtil
{
	/**
	 * Add a varargs list of items to the specified list. If the list or items array are null, then no action is
	 * performed. Note that the destination list has no requirements other than it must be a List of the source item's
	 * type. This allows the destination to be used, for example, as an accumulator.<br>
	 * <br>
	 * Note that this method is not thread safe. Users of this method will need to maintain type safety against the
	 * list.
	 * 
	 * @param list
	 *            A list to which items will be added
	 * @param items
	 *            A list of items to add
	 */
	public static final <T, U extends T> List<T> addToList(List<T> list, U... items)
	{
		if (list != null && items != null)
		{
			list.addAll(Arrays.asList(items));
		}

		return list;
	}

	/**
	 * Add a varargs list of items into a set. If the set or items are null then no action is performed. Note that the
	 * destination set has no requirements other than it must be a Set of the source item's type. This allows the
	 * destination to be used, for example, as an accumulator.<br>
	 * <br>
	 * Note that this method is not thread safe. Users of this method will need to maintain type safety against the set.
	 * 
	 * @param set
	 *            A set to which items will be added
	 * @param items
	 *            A list of items to add
	 */
	public static final <T, U extends T> Set<T> addToSet(Set<T> set, U... items)
	{
		if (set != null && items != null)
		{
			set.addAll(Arrays.asList(items));
		}

		return set;
	}

	/**
	 * Filter a source collection into a destination collection using a filter predicate. If the source or destination
	 * are null, then this is a no-op. If the filter is null, then all source items are added to the destination
	 * collection. Note that the destination collection has no requirements other than it must be a collection of
	 * matching type as the source. This allows the destination to be used, for example, as an accumulator.<br>
	 * <br>
	 * Note that this method is not thread safe. Users of this method will need to maintain type safety against the
	 * collections.
	 * 
	 * @param source
	 *            A collection to filter
	 * @param destination
	 *            A collection to which unfiltered items in source are added
	 * @param filter
	 *            A filter that determines which items to add to the destination collection
	 */
	public static <T> void filter(Collection<T> source, Collection<T> destination, IFilter<? super T> filter)
	{
		if (source != null && destination != null)
		{
			if (filter != null)
			{
				for (T item : source)
				{
					if (filter.include(item))
					{
						destination.add(item);
					}
				}
			}
			else
			{
				destination.addAll(source);
			}
		}
	}

	/**
	 * Generate a new list containing items from the specified collection that the filter determines should be included.
	 * If the specified filter is null, then all items are added to the result list. If the specified collection is null
	 * then an empty list is returned.<br>
	 * <br>
	 * Note that this method is not thread safe. Users of this method will need to maintain type safety against the
	 * collection
	 * 
	 * @param collection
	 *            A collection to filter
	 * @param filter
	 *            A filter that determines which items to keep in the collection
	 * @return Returns a List<T> containing all non-filtered items from the collection
	 */
	public static <T> List<T> filter(Collection<T> collection, IFilter<T> filter)
	{
		ArrayList<T> result = new ArrayList<T>(collection == null ? 0 : collection.size());
		filter(collection, result, filter);
		result.trimToSize();
		return result;
	}

	/**
	 * Filter a collection in place using a filter predicate. If the source or the filter are null, then this is a
	 * no-op. collection.<br>
	 * <br>
	 * Note that this method is not thread safe. Users of this method will need to maintain type safety against the
	 * list.<br>
	 * <br>
	 * Note that not all collections support {@link Iterator#remove()} so it is possible that a
	 * {@link UnsupportedOperationException} can be thrown depending on the collection type
	 * 
	 * @param collection
	 *            A collection to filter
	 * @param filter
	 *            A filter that determines which items to keep in the source collection
	 */
	public static <T> void filterInPlace(Collection<T> collection, IFilter<? super T> filter)
	{
		if (collection != null && filter != null)
		{
			Iterator<T> iterator = collection.iterator();

			while (iterator.hasNext())
			{
				T item = iterator.next();

				if (!filter.include(item))
				{
					iterator.remove();
				}
			}
		}
	}

	/**
	 * This is a convenience method that essentially checks for a null list and returns Collections.emptyList in that
	 * case. If the list is non-null, then this is an identity function.
	 * 
	 * @param <T>
	 * @param list
	 * @return
	 */
	public static <T> List<T> getListValue(List<T> list)
	{
		if (list == null)
		{
			return Collections.emptyList();
		}

		return list;
	}

	/**
	 * This is a convenience method that essentially checks for a null list and returns Collections.emptyList in that
	 * case. If the list is non-null, then this is an identity function.
	 * 
	 * @param <T>
	 * @param <U>
	 * @param list
	 * @return
	 */
	public static <T, U> Map<T, U> getMapValue(Map<T, U> list)
	{
		if (list == null)
		{
			return Collections.emptyMap();
		}

		return list;
	}

	/**
	 * This is a convenience method that essentially checks for a null set and returns Collections.emptySet in that
	 * case. If the set is non-null, then this is an identity function.
	 * 
	 * @param <T>
	 * @param set
	 * @return
	 */
	public static <T> Set<T> getSetValue(Set<T> set)
	{
		if (set == null)
		{
			return Collections.emptySet();
		}

		return set;
	}

	/**
	 * Given two collections of elements of type <T>, return a collection with the items which only appear in one
	 * collection or the other
	 * 
	 * @param <T>
	 *            Type
	 * @param collection1
	 *            Collection #1
	 * @param collection2
	 *            Collection #2
	 * @return Collection with items unique to each list
	 */
	public static <T> Collection<T> getNonOverlapping(Collection<T> collection1, Collection<T> collection2)
	{
		Collection<T> result = union(collection1, collection2);

		result.removeAll(intersect(collection1, collection2));

		return result;
	}

	/**
	 * Given two collections of elements of type <T>, return a collection with the items which only appear in both lists
	 * 
	 * @param <T>
	 *            Type
	 * @param collection1
	 *            Collection #1
	 * @param collection2
	 *            Collection #2
	 * @return Collection with items common to both lists
	 */
	public static <T> Collection<T> intersect(Collection<T> collection1, Collection<T> collection2)
	{
		Set<T> intersection = new HashSet<T>(collection1);

		intersection.retainAll(new HashSet<T>(collection2));

		return intersection;
	}

	/**
	 * This is a convenience method that returns true if the specified collection is null or empty
	 * 
	 * @param <T>
	 *            Any type of object
	 * @param collection
	 * @return
	 */
	public static <T> boolean isEmpty(Collection<T> collection)
	{
		return collection == null || collection.isEmpty();
	}

	/**
	 * This is a convenience method that returns true if the specified map is null or empty
	 * 
	 * @param <T>
	 *            any type of key
	 * @param <U>
	 *            any type of value
	 * @param map
	 * @return
	 */
	public static <T, U> boolean isEmpty(Map<T, U> map)
	{
		return map == null || map.isEmpty();
	}

	/**
	 * Transform the items of a collection to a new type and add to a specified collection. If source, destination, or
	 * mapper are null then no action is performed. Note that the destination collection has no requirements other than
	 * it must be a collection of map's destination type. This allows the destination to be used, for example, as an
	 * accumulator.<br>
	 * <br>
	 * Note that this method is not thread safe. Users of this method will need to maintain type safety against the
	 * collections.
	 * 
	 * @param source
	 *            The collection containing items to be transformed
	 * @param destination
	 *            A collection to which transformed items will be added
	 * @param mapper
	 *            The map that transforms items from their source type to their destination type
	 */
	public static <T, U> void map(Collection<T> source, Collection<U> destination, IMap<? super T, U> mapper)
	{
		if (source != null && destination != null && mapper != null)
		{
			for (T item : source)
			{
				destination.add(mapper.map(item));
			}
		}
	}

	/**
	 * Transform the items of a collection to a new type and add to a new list. If collection or mapper are null then no
	 * action is performed<br>
	 * <br>
	 * Note that this method is not thread safe. Users of this method will need to maintain type safety against the
	 * collection.
	 * 
	 * @param collection
	 *            The collection containing items to be transformed
	 * @param mapper
	 *            The map that transforms items from their source type to their destination type
	 * @return
	 */
	public static <T, U> List<U> map(Collection<T> collection, IMap<? super T, U> mapper)
	{
		List<U> result = new ArrayList<U>();

		map(collection, result, mapper);

		return result;
	}

	/**
	 * Convert a varargs list of items into a Set while preserving order. An empty set is returned if items is null.
	 * 
	 * @param <T>
	 *            Any type of object
	 * @param items
	 *            A variable length list of items of type T
	 * @return Returns a new LinkedHashSet<T> or an empty set
	 */
	public static final <T> Set<T> newInOrderSet(T... items)
	{
		Set<T> result = new LinkedHashSet<T>();

		addToSet(result, items);

		return result;
	}

	/**
	 * Convert a vararg list of items into a List. An empty list is returned if items is null
	 * 
	 * @param <T>
	 *            Any type of object
	 * @param items
	 *            A variable length list of items of type T
	 * @return Returns a new ArrayList<T> or an empty list
	 */
	public static final <T> List<T> newList(T... items)
	{
		List<T> result;

		if (items != null)
		{
			result = new ArrayList<T>();
			addToList(result, items);
		}
		else
		{
			result = Collections.emptyList();
		}

		return result;
	}

	/**
	 * Convert a list of items into a Set. An empty set is returned if items is null
	 * 
	 * @param <T>
	 *            Any type of object
	 * @param items
	 *            A variable length list of items of type T
	 * @return Returns a new HashSet<T> or an empty set
	 */
	public static final <T> Set<T> newSet(T... items)
	{
		Set<T> result;

		if (items != null)
		{
			result = new HashSet<T>();
			addToSet(result, items);
		}
		else
		{
			result = Collections.emptySet();
		}

		return result;
	}

	/**
	 * Convert a list of items into a Set. An empty set is returned if items is null
	 * 
	 * @param <T>
	 *            Any type of object
	 * @param items
	 *            A variable length list of items of type T
	 * @return Returns a new HashSet<T> or an empty set
	 */
	public static final <T> Map<T, T> newMap(T... items)
	{
		Map<T, T> result;

		if (items != null)
		{
			result = new HashMap<T, T>();
			addToMap(result, items);
		}
		else
		{
			result = Collections.emptyMap();
		}

		return result;
	}

	/**
	 * Add a varargs list of items into a map. It is expected that items be in "key, value, key2, value2, etc.."
	 * ordering. If the map or items are null then no action is performed. Note that the destination map has no
	 * requirements other than it must be a Map of the source item's type. This allows the destination to be used, for
	 * example, as an accumulator.<br>
	 * <br>
	 * Note that this method is not thread safe. Users of this method will need to maintain type safety against the map.
	 * 
	 * @param map
	 *            A map to which items will be added
	 * @param items
	 *            A list of items to add
	 */
	public static final <T, U extends T> Map<T, T> addToMap(Map<T, T> map, U... items)
	{
		if (map != null && items != null)
		{
			if (items.length % 2 != 0)
			{
				throw new IllegalArgumentException("Length of list of items must be multiple of 2"); //$NON-NLS-1$
			}
			for (int i = 0; i < items.length; i += 2)
			{
				map.put(items[i], items[i + 1]);
			}
		}

		return map;
	}

	/**
	 * Given a list of elements of type <T>, remove the duplicates from the list in place
	 * 
	 * @param <T>
	 * @param list
	 */
	public static <T> void removeDuplicates(List<T> list)
	{
		// uses LinkedHashSet to keep the order
		Set<T> set = new LinkedHashSet<T>(list);

		list.clear();
		list.addAll(set);
	}

	/**
	 * Given two collections of elements of type <T>, return a collection containing the items from both lists
	 * 
	 * @param <T>
	 *            Type
	 * @param collection1
	 *            Collection #1
	 * @param collection2
	 *            Collection #2
	 * @return Collection with items from both lists
	 */
	public static <T> Collection<T> union(Collection<T> collection1, Collection<T> collection2)
	{
		Set<T> union = new HashSet<T>(collection1);

		union.addAll(new HashSet<T>(collection2));

		return new ArrayList<T>(union);
	}

	private CollectionsUtil()
	{
	}
}
