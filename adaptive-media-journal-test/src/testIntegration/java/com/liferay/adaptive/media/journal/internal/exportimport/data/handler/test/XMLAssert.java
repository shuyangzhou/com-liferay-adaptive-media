/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.adaptive.media.journal.internal.exportimport.data.handler.test;

import com.liferay.portal.kernel.test.AssertUtils;
import com.liferay.portal.kernel.xml.SAXReaderUtil;

/**
 * @author Alejandro Tardín
 */
public class XMLAssert {

	public static void assertEquals(String expectedXML, String actualXML)
		throws Exception {

		AssertUtils.assertEqualsIgnoreCase(
			SAXReaderUtil.read(expectedXML).formattedString(),
			SAXReaderUtil.read(actualXML).formattedString());
	}

}