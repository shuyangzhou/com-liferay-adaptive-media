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

package com.liferay.adaptive.media.image.internal.test;

import com.liferay.adaptive.media.exception.AdaptiveMediaImageConfigurationException.InvalidStateAdaptiveMediaImageConfigurationException;
import com.liferay.adaptive.media.image.configuration.AdaptiveMediaImageConfigurationEntry;
import com.liferay.adaptive.media.image.configuration.AdaptiveMediaImageConfigurationHelper;
import com.liferay.adaptive.media.image.internal.test.util.DestinationReplacer;
import com.liferay.adaptive.media.image.service.AdaptiveMediaImageEntryLocalServiceUtil;
import com.liferay.adaptive.media.internal.messaging.AdaptiveMediaDestinationNames;
import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.test.rule.SynchronousDestinationTestRule;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Sergio González
 */
@RunWith(Arquillian.class)
@Sync(
	destinationNames = {AdaptiveMediaDestinationNames.ADAPTIVE_MEDIA_PROCESSOR}
)
public class AdaptiveMediaImageDeleteConfigurationTest
	extends BaseAdaptiveMediaImageConfigurationTestCase {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			SynchronousDestinationTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		super.setUp();

		_group = GroupTestUtil.addGroup();
	}

	@Test
	public void testDeleteAllConfigurationEntries() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "onedesc", "1",
				properties);

		properties = new HashMap<>();

		properties.put("max-height", "200");
		properties.put("max-width", "200");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "two", "twodesc", "2",
				properties);

		Optional<AdaptiveMediaImageConfigurationEntry>
			firstConfigurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		assertEnabled(firstConfigurationEntryOptional);

		Optional<AdaptiveMediaImageConfigurationEntry>
			secondConfigurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "2");

		assertEnabled(secondConfigurationEntryOptional);

		adaptiveMediaImageConfigurationHelper.
			disableAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		adaptiveMediaImageConfigurationHelper.
			deleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		adaptiveMediaImageConfigurationHelper.
			disableAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "2");

		adaptiveMediaImageConfigurationHelper.
			deleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "2");

		firstConfigurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "1");

		Assert.assertFalse(firstConfigurationEntryOptional.isPresent());

		secondConfigurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "2");

		Assert.assertFalse(secondConfigurationEntryOptional.isPresent());
	}

	@Test
	public void testDeleteConfigurationEntryWithExistingDisabledConfiguration()
		throws Exception {

		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "onedesc", "1",
				properties);

		adaptiveMediaImageConfigurationHelper.
			disableAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		properties = new HashMap<>();

		properties.put("max-height", "200");
		properties.put("max-width", "200");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "two", "twodesc", "2",
				properties);

		adaptiveMediaImageConfigurationHelper.
			disableAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "2");

		adaptiveMediaImageConfigurationHelper.
			deleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "2");

		Optional<AdaptiveMediaImageConfigurationEntry>
			configurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		assertDisabled(configurationEntryOptional);

		configurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "2");

		Assert.assertFalse(configurationEntryOptional.isPresent());
	}

	@Test
	public void testDeleteConfigurationEntryWithImages() throws Exception {
		try (DestinationReplacer destinationReplacer = new DestinationReplacer(
				AdaptiveMediaDestinationNames.ADAPTIVE_MEDIA_PROCESSOR)) {

			AdaptiveMediaImageConfigurationHelper
				adaptiveMediaImageConfigurationHelper =
					serviceTracker.getService();

			Map<String, String> properties = new HashMap<>();

			properties.put("max-height", "100");
			properties.put("max-width", "100");

			AdaptiveMediaImageConfigurationEntry configurationEntry =
				adaptiveMediaImageConfigurationHelper.
					addAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "one", "desc", "1",
						properties);

			FileEntry fileEntry = _addFileEntry();

			FileVersion fileVersion = fileEntry.getFileVersion();

			Assert.assertNotNull(
				AdaptiveMediaImageEntryLocalServiceUtil.
					fetchAdaptiveMediaImageEntry(
						configurationEntry.getUUID(),
						fileVersion.getFileVersionId()));

			adaptiveMediaImageConfigurationHelper.
				disableAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(),
					configurationEntry.getUUID());

			adaptiveMediaImageConfigurationHelper.
				deleteAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(),
					configurationEntry.getUUID());

			Assert.assertNull(
				AdaptiveMediaImageEntryLocalServiceUtil.
					fetchAdaptiveMediaImageEntry(
						configurationEntry.getUUID(),
						fileVersion.getFileVersionId()));
		}
	}

	@Test
	public void testDeleteDeletedConfigurationEntry() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "desc", "1", properties);

		Optional<AdaptiveMediaImageConfigurationEntry>
			configurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		assertEnabled(configurationEntryOptional);

		adaptiveMediaImageConfigurationHelper.
			disableAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		adaptiveMediaImageConfigurationHelper.
			deleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		configurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "1");

		Assert.assertFalse(configurationEntryOptional.isPresent());

		adaptiveMediaImageConfigurationHelper.
			deleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		configurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "1");

		Assert.assertFalse(configurationEntryOptional.isPresent());
	}

	@Test(expected = InvalidStateAdaptiveMediaImageConfigurationException.class)
	public void testDeleteEnabledConfigurationEntry() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "desc", "1", properties);

		Optional<AdaptiveMediaImageConfigurationEntry>
			configurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		assertEnabled(configurationEntryOptional);

		adaptiveMediaImageConfigurationHelper.
			deleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");
	}

	@Test
	public void testDeleteFirstConfigurationEntry() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "onedesc", "1",
				properties);

		properties = new HashMap<>();

		properties.put("max-height", "200");
		properties.put("max-width", "200");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "two", "twodesc", "2",
				properties);

		Optional<AdaptiveMediaImageConfigurationEntry>
			firstConfigurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		assertEnabled(firstConfigurationEntryOptional);

		Optional<AdaptiveMediaImageConfigurationEntry>
			secondConfigurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "2");

		assertEnabled(secondConfigurationEntryOptional);

		adaptiveMediaImageConfigurationHelper.
			disableAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		adaptiveMediaImageConfigurationHelper.
			deleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		firstConfigurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "1");

		Assert.assertFalse(firstConfigurationEntryOptional.isPresent());

		secondConfigurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "2");

		assertEnabled(secondConfigurationEntryOptional);
	}

	@Test
	public void testDeleteSecondConfigurationEntry() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "onedesc", "1",
				properties);

		properties = new HashMap<>();

		properties.put("max-height", "200");
		properties.put("max-width", "200");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "two", "twodesc", "2",
				properties);

		Optional<AdaptiveMediaImageConfigurationEntry>
			firstConfigurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		assertEnabled(firstConfigurationEntryOptional);

		Optional<AdaptiveMediaImageConfigurationEntry>
			secondConfigurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "2");

		assertEnabled(secondConfigurationEntryOptional);

		adaptiveMediaImageConfigurationHelper.
			disableAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "2");

		adaptiveMediaImageConfigurationHelper.
			deleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "2");

		firstConfigurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "1");

		assertEnabled(firstConfigurationEntryOptional);

		secondConfigurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "2");

		Assert.assertFalse(secondConfigurationEntryOptional.isPresent());
	}

	@Test
	public void testDeleteUniqueConfigurationEntry() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "desc", "1", properties);

		Optional<AdaptiveMediaImageConfigurationEntry>
			configurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		assertEnabled(configurationEntryOptional);

		adaptiveMediaImageConfigurationHelper.
			disableAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		adaptiveMediaImageConfigurationHelper.
			deleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		configurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "1");

		Assert.assertFalse(configurationEntryOptional.isPresent());
	}

	@Test
	public void testForceDeleteAllConfigurationEntries() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "onedesc", "1",
				properties);

		properties = new HashMap<>();

		properties.put("max-height", "200");
		properties.put("max-width", "200");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "two", "twodesc", "2",
				properties);

		Optional<AdaptiveMediaImageConfigurationEntry>
			firstConfigurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		assertEnabled(firstConfigurationEntryOptional);

		Optional<AdaptiveMediaImageConfigurationEntry>
			secondConfigurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "2");

		assertEnabled(secondConfigurationEntryOptional);

		adaptiveMediaImageConfigurationHelper.
			forceDeleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		adaptiveMediaImageConfigurationHelper.
			forceDeleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "2");

		firstConfigurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "1");

		Assert.assertFalse(firstConfigurationEntryOptional.isPresent());

		secondConfigurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "2");

		Assert.assertFalse(secondConfigurationEntryOptional.isPresent());
	}

	@Test
	public void testForceDeleteConfigurationEntryWithExistingDisabledConfiguration()
		throws Exception {

		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "onedesc", "1",
				properties);

		adaptiveMediaImageConfigurationHelper.
			disableAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		properties = new HashMap<>();

		properties.put("max-height", "200");
		properties.put("max-width", "200");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "two", "twodesc", "2",
				properties);

		adaptiveMediaImageConfigurationHelper.
			forceDeleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "2");

		Optional<AdaptiveMediaImageConfigurationEntry>
			configurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		assertDisabled(configurationEntryOptional);

		configurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "2");

		Assert.assertFalse(configurationEntryOptional.isPresent());
	}

	@Test
	public void testForceDeleteConfigurationEntryWithImages() throws Exception {
		try (DestinationReplacer destinationReplacer = new DestinationReplacer(
				AdaptiveMediaDestinationNames.ADAPTIVE_MEDIA_PROCESSOR)) {

			AdaptiveMediaImageConfigurationHelper
				adaptiveMediaImageConfigurationHelper =
					serviceTracker.getService();

			Map<String, String> properties = new HashMap<>();

			properties.put("max-height", "100");
			properties.put("max-width", "100");

			AdaptiveMediaImageConfigurationEntry configurationEntry =
				adaptiveMediaImageConfigurationHelper.
					addAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "one", "desc", "1",
						properties);

			FileEntry fileEntry = _addFileEntry();

			FileVersion fileVersion = fileEntry.getFileVersion();

			Assert.assertNotNull(
				AdaptiveMediaImageEntryLocalServiceUtil.
					fetchAdaptiveMediaImageEntry(
						configurationEntry.getUUID(),
						fileVersion.getFileVersionId()));

			adaptiveMediaImageConfigurationHelper.
				forceDeleteAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(),
					configurationEntry.getUUID());

			Assert.assertNull(
				AdaptiveMediaImageEntryLocalServiceUtil.
					fetchAdaptiveMediaImageEntry(
						configurationEntry.getUUID(),
						fileVersion.getFileVersionId()));
		}
	}

	@Test
	public void testForceDeleteDeletedConfigurationEntry() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "desc", "1", properties);

		Optional<AdaptiveMediaImageConfigurationEntry>
			configurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		assertEnabled(configurationEntryOptional);

		adaptiveMediaImageConfigurationHelper.
			forceDeleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		configurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "1");

		Assert.assertFalse(configurationEntryOptional.isPresent());

		adaptiveMediaImageConfigurationHelper.
			forceDeleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		configurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "1");

		Assert.assertFalse(configurationEntryOptional.isPresent());
	}

	@Test
	public void testForceDeleteEnabledConfigurationEntry() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "desc", "1", properties);

		Optional<AdaptiveMediaImageConfigurationEntry>
			configurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		assertEnabled(configurationEntryOptional);

		adaptiveMediaImageConfigurationHelper.
			forceDeleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		configurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "1");

		Assert.assertFalse(configurationEntryOptional.isPresent());
	}

	@Test
	public void testForceDeleteFirstConfigurationEntry() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "onedesc", "1",
				properties);

		properties = new HashMap<>();

		properties.put("max-height", "200");
		properties.put("max-width", "200");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "two", "twodesc", "2",
				properties);

		Optional<AdaptiveMediaImageConfigurationEntry>
			firstConfigurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		assertEnabled(firstConfigurationEntryOptional);

		Optional<AdaptiveMediaImageConfigurationEntry>
			secondConfigurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "2");

		assertEnabled(secondConfigurationEntryOptional);

		adaptiveMediaImageConfigurationHelper.
			forceDeleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		firstConfigurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "1");

		Assert.assertFalse(firstConfigurationEntryOptional.isPresent());

		secondConfigurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "2");

		assertEnabled(secondConfigurationEntryOptional);
	}

	@Test
	public void testForceDeleteSecondConfigurationEntry() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "onedesc", "1",
				properties);

		properties = new HashMap<>();

		properties.put("max-height", "200");
		properties.put("max-width", "200");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "two", "twodesc", "2",
				properties);

		Optional<AdaptiveMediaImageConfigurationEntry>
			firstConfigurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		assertEnabled(firstConfigurationEntryOptional);

		Optional<AdaptiveMediaImageConfigurationEntry>
			secondConfigurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "2");

		assertEnabled(secondConfigurationEntryOptional);

		adaptiveMediaImageConfigurationHelper.
			forceDeleteAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "2");

		firstConfigurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "1");

		assertEnabled(firstConfigurationEntryOptional);

		secondConfigurationEntryOptional =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "2");

		Assert.assertFalse(secondConfigurationEntryOptional.isPresent());
	}

	@Test
	public void testSendsAMessageToTheMessageBus() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		AdaptiveMediaImageConfigurationEntry configurationEntry =
			adaptiveMediaImageConfigurationHelper.
				addAdaptiveMediaImageConfigurationEntry(
					TestPropsValues.getCompanyId(), "one", "onedesc", "1",
					properties);

		adaptiveMediaImageConfigurationHelper.
			disableAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		List<Message> messages = collectConfigurationMessages(
			() ->
				adaptiveMediaImageConfigurationHelper.
					deleteAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1"));

		Assert.assertEquals(messages.toString(), 1, messages.size());

		Message message = messages.get(0);

		AdaptiveMediaImageConfigurationEntry deletedConfigurationEntry =
			(AdaptiveMediaImageConfigurationEntry)message.getPayload();

		Assert.assertEquals(
			configurationEntry.getName(), deletedConfigurationEntry.getName());
		Assert.assertEquals(
			configurationEntry.getDescription(),
			deletedConfigurationEntry.getDescription());
		Assert.assertEquals(
			configurationEntry.getUUID(), deletedConfigurationEntry.getUUID());
		Assert.assertEquals(
			configurationEntry.getProperties(),
			deletedConfigurationEntry.getProperties());
	}

	private FileEntry _addFileEntry() throws Exception {
		return DLAppLocalServiceUtil.addFileEntry(
			TestPropsValues.getUserId(), _group.getGroupId(),
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			StringUtil.randomString() + ".jpg", ContentTypes.IMAGE_JPEG,
			FileUtil.getBytes(
				AdaptiveMediaImageDeleteConfigurationTest.class,
				_PNG_IMAGE_FILE_PATH),
			new ServiceContext());
	}

	private static final String _PNG_IMAGE_FILE_PATH =
		"/com/liferay/adaptive/media/image/internal/test/dependencies" +
			"/image.jpg";

	@DeleteAfterTestRun
	private Group _group;

}