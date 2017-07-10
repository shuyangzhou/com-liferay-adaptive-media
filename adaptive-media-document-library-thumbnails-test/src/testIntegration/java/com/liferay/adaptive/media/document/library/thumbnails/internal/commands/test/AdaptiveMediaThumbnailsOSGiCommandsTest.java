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

package com.liferay.adaptive.media.document.library.thumbnails.internal.commands.test;

import com.liferay.adaptive.media.AdaptiveMedia;
import com.liferay.adaptive.media.document.library.thumbnails.internal.test.util.DestinationReplacer;
import com.liferay.adaptive.media.document.library.thumbnails.internal.test.util.PropsValuesReplacer;
import com.liferay.adaptive.media.image.configuration.AdaptiveMediaImageConfigurationHelper;
import com.liferay.adaptive.media.image.finder.AdaptiveMediaImageFinder;
import com.liferay.adaptive.media.image.processor.AdaptiveMediaImageProcessor;
import com.liferay.adaptive.media.image.service.AdaptiveMediaImageEntryLocalServiceUtil;
import com.liferay.adaptive.media.internal.messaging.AdaptiveMediaDestinationNames;
import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.document.library.kernel.store.DLStoreUtil;
import com.liferay.document.library.kernel.util.DLPreviewableProcessor;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.test.rule.SynchronousDestinationTestRule;
import com.liferay.portal.kernel.test.util.CompanyTestUtil;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.service.test.ServiceTestUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.registry.Registry;
import com.liferay.registry.RegistryUtil;
import com.liferay.registry.ServiceReference;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.util.promise.Promise;

/**
 * @author Adolfo Pérez
 */
@RunWith(Arquillian.class)
@Sync(
	destinationNames = {AdaptiveMediaDestinationNames.ADAPTIVE_MEDIA_PROCESSOR}
)
public class AdaptiveMediaThumbnailsOSGiCommandsTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			SynchronousDestinationTestRule.INSTANCE);

	@BeforeClass
	public static void setUpClass() throws Exception {
		Registry registry = RegistryUtil.getRegistry();

		_adaptiveMediaImageConfigurationHelperServiceReference =
			registry.getServiceReference(
				AdaptiveMediaImageConfigurationHelper.class);
		_adaptiveMediaImageFinderServiceReference =
			registry.getServiceReference(AdaptiveMediaImageFinder.class);

		_adaptiveMediaImageConfigurationHelper = registry.getService(
			_adaptiveMediaImageConfigurationHelperServiceReference);
		_adaptiveMediaImageFinder = registry.getService(
			_adaptiveMediaImageFinderServiceReference);

		_disableAdaptiveMediaThumbnails();
		_disableDocumentLibraryAdaptiveMedia();
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		Registry registry = RegistryUtil.getRegistry();

		registry.ungetService(
			_adaptiveMediaImageConfigurationHelperServiceReference);
		registry.ungetService(_adaptiveMediaImageFinderServiceReference);

		_enableDocumentLibraryAdaptiveMedia();
		_enableAdaptiveMediaThumbnails();
	}

	@Before
	public void setUp() throws Exception {
		ServiceTestUtil.setUser(TestPropsValues.getUser());

		_company = CompanyTestUtil.addCompany();

		_user = UserTestUtil.addCompanyAdminUser(_company);

		_group = GroupTestUtil.addGroup(
			_company.getCompanyId(), _user.getUserId(),
			GroupConstants.DEFAULT_PARENT_GROUP_ID);

		_addConfiguration(100, 100);
		_addConfiguration(300, 300);

		_serviceContext = ServiceContextTestUtil.getServiceContext(
			_group, _user.getUserId());

		ServiceContextThreadLocal.pushServiceContext(_serviceContext);
	}

	@After
	public void tearDown() throws Exception {
		_adaptiveMediaImageConfigurationHelper.
			forceDeleteAdaptiveMediaImageConfigurationEntry(
				_company.getCompanyId(), _THUMBNAIL_CONFIGURATION + 100);

		_adaptiveMediaImageConfigurationHelper.
			forceDeleteAdaptiveMediaImageConfigurationEntry(
				_company.getCompanyId(), _THUMBNAIL_CONFIGURATION + 300);

		FileVersion latestFileVersion = _pngFileEntry.getFileVersion();

		AdaptiveMediaImageEntryLocalServiceUtil.
			deleteAdaptiveMediaImageEntryFileVersion(latestFileVersion);

		GroupLocalServiceUtil.deleteGroup(_group);

		CompanyLocalServiceUtil.deleteCompany(_company);

		ServiceContextThreadLocal.popServiceContext();
	}

	@Test
	public void testCleanUpDeletesImageThumbnails() throws Exception {
		try (DestinationReplacer destinationReplacer = new DestinationReplacer(
				DestinationNames.DOCUMENT_LIBRARY_IMAGE_PROCESSOR,
				AdaptiveMediaDestinationNames.ADAPTIVE_MEDIA_PROCESSOR)) {

			int count = _getThumbnailCount();

			_addPNGFileEntry();

			Assert.assertEquals(count + 1, _getThumbnailCount());

			_cleanUp();

			Assert.assertEquals(count, _getThumbnailCount());
		}
	}

	@Test
	public void testCleanUpDeletesOnlyImageThumbnails() throws Exception {
		try (DestinationReplacer destinationReplacer = new DestinationReplacer(
				DestinationNames.DOCUMENT_LIBRARY_IMAGE_PROCESSOR,
				AdaptiveMediaDestinationNames.ADAPTIVE_MEDIA_PROCESSOR)) {

			int count = _getThumbnailCount();

			_addPDFFileEntry();
			_addPNGFileEntry();

			Assert.assertEquals(count + 2, _getThumbnailCount());

			_cleanUp();

			Assert.assertEquals(count + 1, _getThumbnailCount());
		}
	}

	@Test
	public void testMigrateDoesNotRemoveThumbnails() throws Exception {
		try (DestinationReplacer destinationReplacer = new DestinationReplacer(
				DestinationNames.DOCUMENT_LIBRARY_IMAGE_PROCESSOR,
				AdaptiveMediaDestinationNames.ADAPTIVE_MEDIA_PROCESSOR)) {

			int count = _getThumbnailCount();

			_addPDFFileEntry();
			_addPNGFileEntry();

			Assert.assertEquals(count + 2, _getThumbnailCount());

			_migrate();

			Assert.assertEquals(count + 2, _getThumbnailCount());
		}
	}

	@Test
	public void testMigrateOnlyProcessesImages() throws Exception {
		try (DestinationReplacer destinationReplacer = new DestinationReplacer(
				DestinationNames.DOCUMENT_LIBRARY_IMAGE_PROCESSOR,
				AdaptiveMediaDestinationNames.ADAPTIVE_MEDIA_PROCESSOR);
			PropsValuesReplacer propsValuesReplacer1 = new PropsValuesReplacer(
				"DL_FILE_ENTRY_THUMBNAIL_CUSTOM_1_MAX_HEIGHT", 100);
			PropsValuesReplacer propsValuesReplacer2 = new PropsValuesReplacer(
				"DL_FILE_ENTRY_THUMBNAIL_CUSTOM_1_MAX_WIDTH", 100)) {

			FileEntry pdfFileEntry = _addPDFFileEntry();
			FileEntry pngFileEntry = _addPNGFileEntry();

			_migrate();

			Assert.assertEquals(0, _getAdaptiveMediaCount(pdfFileEntry));
			Assert.assertEquals(2, _getAdaptiveMediaCount(pngFileEntry));
		}
	}

	@Test(expected = InvocationTargetException.class)
	public void testMigrateThrowsExceptionWhenNoValidConfiguration()
		throws Exception {

		try (DestinationReplacer destinationReplacer = new DestinationReplacer(
				DestinationNames.DOCUMENT_LIBRARY_IMAGE_PROCESSOR,
				AdaptiveMediaDestinationNames.ADAPTIVE_MEDIA_PROCESSOR);
			PropsValuesReplacer propsValuesReplacer1 = new PropsValuesReplacer(
				"DL_FILE_ENTRY_THUMBNAIL_MAX_HEIGHT", 999);
			PropsValuesReplacer propsValuesReplacer2 = new PropsValuesReplacer(
				"DL_FILE_ENTRY_THUMBNAIL_MAX_HEIGHT", 999)) {

			_addPNGFileEntry();

			_migrate();
		}
	}

	private static void _disableAdaptiveMediaThumbnails() throws Exception {
		Registry registry = RegistryUtil.getRegistry();

		ServiceComponentRuntime serviceComponentRuntime = registry.getService(
			ServiceComponentRuntime.class);

		Object service = registry.getService(_PROCESSOR_CLASS_NAME);

		if (service == null) {
			return;
		}

		Bundle bundle = FrameworkUtil.getBundle(service.getClass());

		ComponentDescriptionDTO componentDescriptionDTO =
			serviceComponentRuntime.getComponentDescriptionDTO(
				bundle, _PROCESSOR_CLASS_NAME);

		if (componentDescriptionDTO == null) {
			return;
		}

		Promise<Void> promise = serviceComponentRuntime.disableComponent(
			componentDescriptionDTO);

		promise.getValue();
	}

	private static void _disableDocumentLibraryAdaptiveMedia()
		throws BundleException {

		Bundle bundle = FrameworkUtil.getBundle(
			AdaptiveMediaThumbnailsOSGiCommandsTest.class);

		BundleContext bundleContext = bundle.getBundleContext();

		for (Bundle curBundle : bundleContext.getBundles()) {
			if (_BUNDLE_SYMBOLIC_NAME.equals(curBundle.getSymbolicName())) {
				if (curBundle.getState() == Bundle.ACTIVE) {
					curBundle.stop();
				}

				break;
			}
		}
	}

	private static void _enableAdaptiveMediaThumbnails() throws Exception {
		Registry registry = RegistryUtil.getRegistry();

		ServiceComponentRuntime serviceComponentRuntime = registry.getService(
			ServiceComponentRuntime.class);

		Object service = registry.getService(_COMMAND_CLASS_NAME);

		Bundle bundle = FrameworkUtil.getBundle(service.getClass());

		ComponentDescriptionDTO componentDescriptionDTO =
			serviceComponentRuntime.getComponentDescriptionDTO(
				bundle, _PROCESSOR_CLASS_NAME);

		if (componentDescriptionDTO == null) {
			return;
		}

		Promise<Void> promise = serviceComponentRuntime.enableComponent(
			componentDescriptionDTO);

		promise.getValue();
	}

	private static void _enableDocumentLibraryAdaptiveMedia()
		throws BundleException {

		Bundle bundle = FrameworkUtil.getBundle(
			AdaptiveMediaThumbnailsOSGiCommandsTest.class);

		BundleContext bundleContext = bundle.getBundleContext();

		for (Bundle curBundle : bundleContext.getBundles()) {
			if (_BUNDLE_SYMBOLIC_NAME.equals(curBundle.getSymbolicName())) {
				if (curBundle.getState() != Bundle.ACTIVE) {
					curBundle.start();
				}

				break;
			}
		}
	}

	private void _addConfiguration(int width, int height) throws Exception {
		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", String.valueOf(height));
		properties.put("max-width", String.valueOf(width));

		_adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				_company.getCompanyId(), _THUMBNAIL_CONFIGURATION + width,
				StringPool.BLANK, _THUMBNAIL_CONFIGURATION + width, properties);
	}

	private FileEntry _addPDFFileEntry() throws Exception {
		return DLAppLocalServiceUtil.addFileEntry(
			_user.getUserId(), _group.getGroupId(),
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString() + ".pdf", ContentTypes.APPLICATION_PDF,
			_getFileContents("sample.pdf"), _serviceContext);
	}

	private FileEntry _addPNGFileEntry() throws Exception {
		_pngFileEntry = DLAppLocalServiceUtil.addFileEntry(
			_user.getUserId(), _group.getGroupId(),
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString() + ".png", ContentTypes.IMAGE_PNG,
			_getFileContents("sample.png"), _serviceContext);

		return _pngFileEntry;
	}

	private void _cleanUp() throws Exception {
		_run("cleanUp");
	}

	private long _getAdaptiveMediaCount(FileEntry fileEntry) throws Exception {
		Stream<AdaptiveMedia<AdaptiveMediaImageProcessor>> adaptiveMediaStream =
			_adaptiveMediaImageFinder.getAdaptiveMediaStream(
				queryBuilder -> queryBuilder.allForFileEntry(
					fileEntry
				).done());

		return adaptiveMediaStream.count();
	}

	private byte[] _getFileContents(String fileName) throws Exception {
		return FileUtil.getBytes(
			AdaptiveMediaThumbnailsOSGiCommandsTest.class,
			"/com/liferay/adaptive/media/document/library/thumbnails/internal" +
				"/commands/test/dependencies/" + fileName);
	}

	private int _getThumbnailCount() throws Exception {
		String[] fileNames = DLStoreUtil.getFileNames(
			_company.getCompanyId(), DLPreviewableProcessor.REPOSITORY_ID,
			DLPreviewableProcessor.THUMBNAIL_PATH);

		return fileNames.length;
	}

	private void _migrate() throws Exception {
		_run("migrate");
	}

	private void _run(String functionName) throws Exception {
		Registry registry = RegistryUtil.getRegistry();

		Object service = registry.getService(_COMMAND_CLASS_NAME);

		Class<?> clazz = service.getClass();

		Method method = clazz.getMethod(functionName, String[].class);

		method.invoke(
			service,
			(Object)new String[] {String.valueOf(_company.getCompanyId())});
	}

	private static final String _BUNDLE_SYMBOLIC_NAME =
		"com.liferay.adaptive.media.document.library";

	private static final String _COMMAND_CLASS_NAME =
		"com.liferay.adaptive.media.document.library.thumbnails.internal." +
			"commands.AdaptiveMediaThumbnailsOSGiCommands";

	private static final String _PROCESSOR_CLASS_NAME =
		"com.liferay.adaptive.media.document.library.thumbnails.internal." +
			"AdaptiveMediaImageEntryProcessor";

	private static final String _THUMBNAIL_CONFIGURATION = "thumbnail";

	private static AdaptiveMediaImageConfigurationHelper
		_adaptiveMediaImageConfigurationHelper;
	private static ServiceReference<AdaptiveMediaImageConfigurationHelper>
		_adaptiveMediaImageConfigurationHelperServiceReference;
	private static AdaptiveMediaImageFinder _adaptiveMediaImageFinder;
	private static ServiceReference<AdaptiveMediaImageFinder>
		_adaptiveMediaImageFinderServiceReference;

	private Company _company;
	private Group _group;
	private FileEntry _pngFileEntry;
	private ServiceContext _serviceContext;
	private User _user;

}