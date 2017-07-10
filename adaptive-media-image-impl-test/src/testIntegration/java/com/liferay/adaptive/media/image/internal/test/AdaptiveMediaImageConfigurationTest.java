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

import com.liferay.adaptive.media.image.configuration.AdaptiveMediaImageConfigurationEntry;
import com.liferay.adaptive.media.image.configuration.AdaptiveMediaImageConfigurationHelper;
import com.liferay.adaptive.media.internal.messaging.AdaptiveMediaDestinationNames;
import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.test.rule.SynchronousDestinationTestRule;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
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
public class AdaptiveMediaImageConfigurationTest
	extends BaseAdaptiveMediaImageConfigurationTestCase {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			SynchronousDestinationTestRule.INSTANCE);

	@Test
	public void testEmptyConfiguration() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Iterable<AdaptiveMediaImageConfigurationEntry> configurationEntries =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntries(
					TestPropsValues.getCompanyId());

		Iterator<AdaptiveMediaImageConfigurationEntry> iterator =
			configurationEntries.iterator();

		Assert.assertFalse(iterator.hasNext());
	}

	@Test
	public void testExistantConfigurationEntry() throws Exception {
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

		Assert.assertTrue(configurationEntryOptional.isPresent());
	}

	@Test
	public void testGetConfigurationEntriesDoesNotReturnDisabledConfigurations()
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

		Collection<AdaptiveMediaImageConfigurationEntry> configurationEntries =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntries(
					TestPropsValues.getCompanyId());

		Assert.assertEquals(
			configurationEntries.toString(), 1, configurationEntries.size());

		Iterator<AdaptiveMediaImageConfigurationEntry> iterator =
			configurationEntries.iterator();

		AdaptiveMediaImageConfigurationEntry configurationEntry =
			iterator.next();

		Assert.assertEquals("1", configurationEntry.getUUID());
	}

	@Test
	public void testGetConfigurationEntriesWithFilterReturnsDisabledConfigurations()
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

		Collection<AdaptiveMediaImageConfigurationEntry> configurationEntries =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntries(
					TestPropsValues.getCompanyId(), configurationEntry -> true);

		Assert.assertEquals(
			configurationEntries.toString(), 2, configurationEntries.size());

		Iterator<AdaptiveMediaImageConfigurationEntry> iterator =
			configurationEntries.iterator();

		AdaptiveMediaImageConfigurationEntry configurationEntry =
			iterator.next();

		Assert.assertEquals("1", configurationEntry.getUUID());

		configurationEntry = iterator.next();

		Assert.assertEquals("2", configurationEntry.getUUID());
	}

	@Test
	public void testGetConfigurationEntryReturnsDisabledConfiguration()
		throws Exception {

		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "desc", "1", properties);

		adaptiveMediaImageConfigurationHelper.
			disableAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "1");

		Optional<AdaptiveMediaImageConfigurationEntry>
			configurationEntryOptional =
				adaptiveMediaImageConfigurationHelper.
					getAdaptiveMediaImageConfigurationEntry(
						TestPropsValues.getCompanyId(), "1");

		Assert.assertTrue(configurationEntryOptional.isPresent());
	}

	@Test
	public void testNonEmptyConfiguration() throws Exception {
		AdaptiveMediaImageConfigurationHelper
			adaptiveMediaImageConfigurationHelper = serviceTracker.getService();

		Map<String, String> properties = new HashMap<>();

		properties.put("max-height", "100");
		properties.put("max-width", "100");

		adaptiveMediaImageConfigurationHelper.
			addAdaptiveMediaImageConfigurationEntry(
				TestPropsValues.getCompanyId(), "one", "desc", "1", properties);

		Collection<AdaptiveMediaImageConfigurationEntry> configurationEntries =
			adaptiveMediaImageConfigurationHelper.
				getAdaptiveMediaImageConfigurationEntries(
					TestPropsValues.getCompanyId());

		Assert.assertFalse(configurationEntries.isEmpty());
	}

	@Test
	public void testNonExistantConfigurationEntry() throws Exception {
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
						TestPropsValues.getCompanyId(), "0");

		Assert.assertFalse(configurationEntryOptional.isPresent());
	}

}