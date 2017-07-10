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

package com.liferay.adaptive.media.document.library.thumbnails.internal.commands;

import com.liferay.adaptive.media.image.configuration.AdaptiveMediaImageConfigurationEntry;
import com.liferay.adaptive.media.image.configuration.AdaptiveMediaImageConfigurationHelper;
import com.liferay.adaptive.media.image.constants.AdaptiveMediaImageConstants;
import com.liferay.adaptive.media.image.model.AdaptiveMediaImageEntry;
import com.liferay.adaptive.media.image.service.AdaptiveMediaImageEntryLocalService;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.kernel.store.DLStoreUtil;
import com.liferay.document.library.kernel.util.DLPreviewableProcessor;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.image.ImageBag;
import com.liferay.portal.kernel.image.ImageToolUtil;
import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayInputStream;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;

import java.awt.image.RenderedImage;

import java.io.IOException;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Adolfo Pérez
 */
@Component(
	immediate = true,
	property = {
		"osgi.command.function=check", "osgi.command.function=cleanUp",
		"osgi.command.function=migrate", "osgi.command.scope=thumbnails"
	},
	service = AdaptiveMediaThumbnailsOSGiCommands.class
)
public class AdaptiveMediaThumbnailsOSGiCommands {

	public void check(String... companyIds) {
		System.out.println("Company ID\t# of thumbnails pending migration");
		System.out.println("-------------------------------------------------");

		int total = 0;

		for (long companyId : _getCompanyIds(companyIds)) {
			try {
				String[] fileNames = DLStoreUtil.getFileNames(
					companyId, DLPreviewableProcessor.REPOSITORY_ID,
					DLPreviewableProcessor.THUMBNAIL_PATH);

				int companyTotal = 0;

				for (String fileName : fileNames) {

					// See LPS-70788

					String actualFileName = StringUtil.replace(
						fileName, "//", StringPool.SLASH);

					for (ThumbnailConfiguration thumbnailConfiguration :
							_getThumbnailConfigurations()) {

						FileVersion fileVersion = _getFileVersion(
							thumbnailConfiguration.getFileVersionId(
								actualFileName));

						if (fileVersion != null) {
							companyTotal += 1;
						}
					}
				}

				System.out.printf("%d\t\t%d%n", companyId, companyTotal);

				total += companyTotal;
			}
			catch (Exception e) {
				_log.error(e);
			}
		}

		System.out.printf("%nTOTAL: %d%n", total);
	}

	public void cleanUp(String... companyIds) {
		for (long companyId : _getCompanyIds(companyIds)) {
			try {
				String[] fileNames = DLStoreUtil.getFileNames(
					companyId, DLPreviewableProcessor.REPOSITORY_ID,
					DLPreviewableProcessor.THUMBNAIL_PATH);

				for (String fileName : fileNames) {

					// See LPS-70788

					String actualFileName = StringUtil.replace(
						fileName, "//", StringPool.SLASH);

					for (ThumbnailConfiguration thumbnailConfiguration :
							_getThumbnailConfigurations()) {

						FileVersion fileVersion = _getFileVersion(
							thumbnailConfiguration.getFileVersionId(
								actualFileName));

						if (fileVersion != null) {
							DLStoreUtil.deleteFile(
								companyId, DLPreviewableProcessor.REPOSITORY_ID,
								actualFileName);
						}
					}
				}
			}
			catch (Exception e) {
				_log.error(e);
			}
		}
	}

	public void migrate(String... companyIds) throws PortalException {
		for (long companyId : _getCompanyIds(companyIds)) {
			Collection<AdaptiveMediaImageConfigurationEntry>
				configurationEntries =
					_adaptiveMediaImageConfigurationHelper.
						getAdaptiveMediaImageConfigurationEntries(companyId);

			if (!_isValidConfigurationEntries(configurationEntries)) {
				throw new PortalException(
					"No valid Adaptive Media configuration found. Please " +
						"refer to the upgrade documentation for the details.");
			}

			try {
				String[] fileNames = DLStoreUtil.getFileNames(
					companyId, DLPreviewableProcessor.REPOSITORY_ID,
					DLPreviewableProcessor.THUMBNAIL_PATH);

				for (String fileName : fileNames) {

					// See LPS-70788

					String actualFileName = StringUtil.replace(
						fileName, "//", StringPool.SLASH);

					for (ThumbnailConfiguration thumbnailConfiguration :
							_getThumbnailConfigurations()) {

						Optional<AdaptiveMediaImageConfigurationEntry>
							configurationEntryOptional =
								thumbnailConfiguration.
									selectMatchingConfigurationEntry(
										configurationEntries);

						configurationEntryOptional.ifPresent(
							configurationEntry -> _migrate(
								actualFileName, configurationEntry,
								thumbnailConfiguration));
					}
				}
			}
			catch (PortalException pe) {
				_log.error(pe);
			}
		}
	}

	private Iterable<Long> _getCompanyIds(String... companyIds) {
		if (companyIds.length == 0) {
			List<Company> companies = _companyLocalService.getCompanies();

			Stream<Company> companyStream = companies.stream();

			return companyStream.map(
				Company::getCompanyId
			).collect(
				Collectors.toList()
			);
		}

		Stream<String> companyIdStream = Arrays.stream(companyIds);

		return companyIdStream.map(
			Long::parseLong
		).collect(
			Collectors.toList()
		);
	}

	private FileVersion _getFileVersion(long fileVersionId)
		throws PortalException {

		try {
			if (fileVersionId == 0) {
				return null;
			}

			FileVersion fileVersion = _dlAppLocalService.getFileVersion(
				fileVersionId);

			if (!_isMimeTypeSupported(fileVersion)) {
				return null;
			}

			return fileVersion;
		}
		catch (PortalException pe) {
			_log.error(pe);

			return null;
		}
	}

	private ThumbnailConfiguration[] _getThumbnailConfigurations() {
		return new ThumbnailConfiguration[] {
			new ThumbnailConfiguration(
				PrefsPropsUtil.getInteger(
					PropsKeys.DL_FILE_ENTRY_THUMBNAIL_MAX_WIDTH),
				PrefsPropsUtil.getInteger(
					PropsKeys.DL_FILE_ENTRY_THUMBNAIL_MAX_HEIGHT),
				Pattern.compile(
					DLPreviewableProcessor.THUMBNAIL_PATH +
						"\\d+/\\d+(?:/\\d+)+/(\\d+)(?:\\..+)?$")),
			new ThumbnailConfiguration(
				PrefsPropsUtil.getInteger(
					PropsKeys.DL_FILE_ENTRY_THUMBNAIL_CUSTOM_1_MAX_WIDTH),
				PrefsPropsUtil.getInteger(
					PropsKeys.DL_FILE_ENTRY_THUMBNAIL_CUSTOM_1_MAX_HEIGHT),
				Pattern.compile(
					DLPreviewableProcessor.THUMBNAIL_PATH +
						"\\d+/\\d+(?:/\\d+)+/(\\d+)-1(?:\\..+)?$")),
			new ThumbnailConfiguration(
				PrefsPropsUtil.getInteger(
					PropsKeys.DL_FILE_ENTRY_THUMBNAIL_CUSTOM_2_MAX_WIDTH),
				PrefsPropsUtil.getInteger(
					PropsKeys.DL_FILE_ENTRY_THUMBNAIL_CUSTOM_2_MAX_HEIGHT),
				Pattern.compile(
					DLPreviewableProcessor.THUMBNAIL_PATH +
						"\\d+/\\d+(?:/\\d+)+/(\\d+)-2(?:\\..+)?$"))
		};
	}

	private boolean _isMimeTypeSupported(FileVersion fileVersion) {
		Set<String> supportedMimeTypes =
			AdaptiveMediaImageConstants.getSupportedMimeTypes();

		return supportedMimeTypes.contains(fileVersion.getMimeType());
	}

	private boolean _isValidConfigurationEntries(
		Collection<AdaptiveMediaImageConfigurationEntry> configurationEntries) {

		Stream<ThumbnailConfiguration> thumbnailConfigurationStream =
			Arrays.stream(_getThumbnailConfigurations());

		return thumbnailConfigurationStream.anyMatch(
			thumbnailConfiguration -> {
				Stream<AdaptiveMediaImageConfigurationEntry>
					adaptiveMediaImageConfigurationEntryStream =
						configurationEntries.stream();

				return adaptiveMediaImageConfigurationEntryStream.anyMatch(
					thumbnailConfiguration::matches);
			});
	}

	private void _migrate(
		String fileName,
		AdaptiveMediaImageConfigurationEntry configurationEntry,
		ThumbnailConfiguration thumbnailConfiguration) {

		try {
			FileVersion fileVersion = _getFileVersion(
				thumbnailConfiguration.getFileVersionId(fileName));

			if (fileVersion == null) {
				return;
			}

			AdaptiveMediaImageEntry adaptiveMediaImageEntry =
				_adaptiveMediaImageEntryLocalService.
					fetchAdaptiveMediaImageEntry(
						configurationEntry.getUUID(),
						fileVersion.getFileVersionId());

			if (adaptiveMediaImageEntry != null) {
				return;
			}

			byte[] bytes = DLStoreUtil.getFileAsBytes(
				fileVersion.getCompanyId(),
				DLPreviewableProcessor.REPOSITORY_ID, fileName);

			ImageBag imageBag = ImageToolUtil.read(bytes);

			RenderedImage renderedImage = imageBag.getRenderedImage();

			_adaptiveMediaImageEntryLocalService.addAdaptiveMediaImageEntry(
				configurationEntry, fileVersion, renderedImage.getWidth(),
				renderedImage.getHeight(),
				new UnsyncByteArrayInputStream(bytes), bytes.length);
		}
		catch (IOException | PortalException e) {
			_log.error(e);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		AdaptiveMediaThumbnailsOSGiCommands.class);

	@Reference
	private AdaptiveMediaImageConfigurationHelper
		_adaptiveMediaImageConfigurationHelper;

	@Reference
	private AdaptiveMediaImageEntryLocalService
		_adaptiveMediaImageEntryLocalService;

	@Reference
	private CompanyLocalService _companyLocalService;

	@Reference
	private DLAppLocalService _dlAppLocalService;

}