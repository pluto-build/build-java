package build.pluto.buildjava.util;

import java.util.List;

public final class ListUtils {
	public static <T> String printList(List<T> list) {
		return printList(list, "[", "]", ", ");
	}
	
	public static <T> String printList(List<T> list, String prefix, String postfix, String separator) {
		StringBuilder sb = new StringBuilder(prefix);
		
		for (T t: list) {
			sb.append(t).append(separator);
		}
		if (sb.toString().endsWith(separator))
			return sb.toString().substring(0, sb.length()-separator.length()) + postfix;
		
		return sb.append(postfix).toString();
	}

	public static <T> boolean equals(List<T> list1, List<T> list2) {
		if (list1 == null && list2 == null) {
			return true;
		}
		if (list1 == null || list2 == null) {
			return false;
		}
		for (int i = 0; i < list1.size(); i++) {
			if (!list1.get(i).equals(list2.get(i))) {
				return false;
			}
		}
		return true;
	}

	public static <T> boolean equalsEmptyEqNull(List<T> list1, List<T> list2) {
		if ((list1 == null || list1.isEmpty()) && (list2 == null || list2.isEmpty())) {
			return true;
		}
		if (list1 == null || list2 == null) {
			return false;
		}
		for (int i = 0; i < list1.size(); i++) {
			if (!list1.get(i).equals(list2.get(i))) {
				return false;
			}
		}
		return true;
	}
}
