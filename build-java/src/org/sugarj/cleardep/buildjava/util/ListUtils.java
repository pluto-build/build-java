package org.sugarj.cleardep.buildjava.util;

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
}
