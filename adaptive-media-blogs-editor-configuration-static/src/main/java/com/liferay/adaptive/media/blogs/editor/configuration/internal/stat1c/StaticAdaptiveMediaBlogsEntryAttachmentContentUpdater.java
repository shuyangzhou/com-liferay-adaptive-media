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

package com.liferay.adaptive.media.blogs.editor.configuration.internal.stat1c;

import com.liferay.adaptive.media.image.html.AdaptiveMediaImageHTMLTagFactory;
import com.liferay.blogs.util.BlogsEntryAttachmentContentUpdater;
import com.liferay.portal.kernel.repository.model.FileEntry;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alejandro Tardín
 */
@Component(
	property = "service.ranking:Integer=1",
	service = BlogsEntryAttachmentContentUpdater.class
)
public class StaticAdaptiveMediaBlogsEntryAttachmentContentUpdater
	extends BlogsEntryAttachmentContentUpdater {

	@Override
	protected String getBlogsEntryAttachmentFileEntryImgTag(
		FileEntry blogsEntryAttachmentFileEntry) {

		try {
			String imgTag = super.getBlogsEntryAttachmentFileEntryImgTag(
				blogsEntryAttachmentFileEntry);

			return _adaptiveMediaImageHTMLTagFactory.create(
				imgTag, blogsEntryAttachmentFileEntry);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Reference(unbind = "-")
	protected void setAdaptiveMediaImageHTMLTagFactory(
		AdaptiveMediaImageHTMLTagFactory adaptiveMediaImageHTMLTagFactory) {

		_adaptiveMediaImageHTMLTagFactory = adaptiveMediaImageHTMLTagFactory;
	}

	private AdaptiveMediaImageHTMLTagFactory _adaptiveMediaImageHTMLTagFactory;

}