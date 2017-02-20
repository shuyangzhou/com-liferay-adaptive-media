/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.adaptive.media.image.web.processor.content;

import com.liferay.adaptive.media.AdaptiveMedia;
import com.liferay.adaptive.media.AdaptiveMediaException;
import com.liferay.adaptive.media.image.finder.ImageAdaptiveMediaFinder;
import com.liferay.adaptive.media.image.processor.ImageAdaptiveMediaAttribute;
import com.liferay.adaptive.media.image.processor.ImageAdaptiveMediaProcessor;
import com.liferay.adaptive.media.processor.content.ConcreteContentProcessor;
import com.liferay.adaptive.media.processor.content.ContentType;
import com.liferay.adaptive.media.web.constants.ContentTypes;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.util.StringBundler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alejandro Tardín
 */
@Component(immediate = true, service = ConcreteContentProcessor.class)
public class HTMLContentProcessorImpl
	implements ConcreteContentProcessor<String> {

	@Override
	public ContentType<String> getContentType() {
		return ContentTypes.HTML;
	}

	@Override
	public String process(String html)
		throws AdaptiveMediaException, PortalException {

		StringBuffer sb = new StringBuffer(html.length());
		Matcher matcher = _IMG_PATTERN.matcher(html);

		while (matcher.find()) {
			String picture = _getPictureElement(matcher.group(0));

			matcher.appendReplacement(sb, Matcher.quoteReplacement(picture));
		}

		matcher.appendTail(sb);

		return sb.toString();
	}

	@Reference(unbind = "-")
	public void setDlAppLocalService(DLAppLocalService dlAppLocalService) {
		_dlAppLocalService = dlAppLocalService;
	}

	@Reference(
		target = "(model.class.name=com.liferay.portal.kernel.repository.model.FileVersion)",
		unbind = "-"
	)
	public void setImageAdaptiveMediaFinder(
		ImageAdaptiveMediaFinder imageAdaptiveMediaFinder) {

		_imageAdaptiveMediaFinder = imageAdaptiveMediaFinder;
	}

	private Stream<AdaptiveMedia<ImageAdaptiveMediaProcessor>>
			_getAdaptiveMedias(long fileEntryId)
		throws AdaptiveMediaException, PortalException {

		FileEntry fileEntry = _dlAppLocalService.getFileEntry(fileEntryId);

		return _imageAdaptiveMediaFinder.getAdaptiveMedia(
			queryBuilder -> queryBuilder.allForFileEntry(
				fileEntry).orderBy(
					ImageAdaptiveMediaAttribute.IMAGE_WIDTH, true).done());
	}

	private Optional<String> _getMediaQuery(
		AdaptiveMedia<ImageAdaptiveMediaProcessor> adaptiveMedia,
		AdaptiveMedia<ImageAdaptiveMediaProcessor> previousAdaptiveMedia) {

		return _getWidth(adaptiveMedia).map(maxWidth -> {
			String constraints = "max-width:" + maxWidth + "px";

			if (previousAdaptiveMedia != null) {
				Optional<Integer> optionalWidth = _getWidth(
					previousAdaptiveMedia);

				constraints += optionalWidth.map(
					previousMaxWidth ->
						" and min-width:" + previousMaxWidth + "px").orElse("");
			}

			return "(" + constraints + ")";
		});
	}

	private String _getPictureElement(String img)
		throws AdaptiveMediaException, PortalException {

		Matcher matcher = _FILE_ENTRY_ID_PATTERN.matcher(img);

		if (matcher.matches()) {
			Long fileEntryId = Long.valueOf(matcher.group(1));

			List<AdaptiveMedia<ImageAdaptiveMediaProcessor>> adaptiveMediaList =
				_getAdaptiveMedias(fileEntryId).collect(Collectors.toList());

			StringBundler sb = new StringBundler(3 + adaptiveMediaList.size());

			sb.append("<picture>");
			_getSourceElements(adaptiveMediaList).forEach(sb::append);
			sb.append(img);
			sb.append("</picture>");

			return sb.toString();
		}

		return img;
	}

	private String _getSourceElement(
		AdaptiveMedia<ImageAdaptiveMediaProcessor> previousAdaptiveMedia,
		AdaptiveMedia<ImageAdaptiveMediaProcessor> adaptiveMedia) {

		StringBundler sb = new StringBundler(8);

		sb.append("<source");

		_getMediaQuery(adaptiveMedia, previousAdaptiveMedia).ifPresent(
			mediaQuery -> {
				sb.append(" media=\"");
				sb.append(mediaQuery);
				sb.append("\"");
			});

		sb.append(" srcset=\"");
		sb.append(adaptiveMedia.getURI());
		sb.append("\"");

		sb.append("/>");

		return sb.toString();
	}

	private List<String> _getSourceElements(
			List<AdaptiveMedia<ImageAdaptiveMediaProcessor>> adaptiveMediaList)
		throws AdaptiveMediaException, PortalException {

		List<String> sourceElements = new ArrayList<>();

		AdaptiveMedia previousAdaptiveMedia = null;

		for (AdaptiveMedia<ImageAdaptiveMediaProcessor> adaptiveMedia :
				adaptiveMediaList) {

			sourceElements.add(
				_getSourceElement(previousAdaptiveMedia, adaptiveMedia));

			previousAdaptiveMedia = adaptiveMedia;
		}

		return sourceElements;
	}

	private Optional<Integer> _getWidth(
		AdaptiveMedia<ImageAdaptiveMediaProcessor> previousAdaptiveMedia) {

		return previousAdaptiveMedia.getAttributeValue(
			ImageAdaptiveMediaAttribute.IMAGE_WIDTH);
	}

	private static final String _ADAPTIVE_ATTR = "data-fileEntryId";

	private static final Pattern _FILE_ENTRY_ID_PATTERN = Pattern.compile(
		"^<img .*?" + _ADAPTIVE_ATTR + "=\"([0-9]*)\".*?/>$",
		Pattern.CASE_INSENSITIVE);

	private static final Pattern _IMG_PATTERN = Pattern.compile("<img.*?/>");

	private DLAppLocalService _dlAppLocalService;
	private ImageAdaptiveMediaFinder _imageAdaptiveMediaFinder;

}