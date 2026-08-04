/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileManagerTest {

	@TempDir
	Path tempDir;

	private Path write(String content) throws Exception {
		Path file = tempDir.resolve("profiles.properties");
		Files.writeString(file, content);
		return file;
	}

	@Test
	void workingDirConfigWinsOverHomeConfig() throws Exception {
		Path workingDir = Files.createDirectories(tempDir.resolve("install"));
		Path home = Files.createDirectories(tempDir.resolve("home"));

		// Neither exists: the home path is the default (that is where a new file gets created).
		assertThat(ProfileManager.resolveConfigFile(workingDir, home))
				.isEqualTo(home.resolve(".matrosdms").resolve("profiles.properties"));

		Files.writeString(workingDir.resolve("profiles.properties"), "profile.x.data-dir=D:/x\n");
		assertThat(ProfileManager.resolveConfigFile(workingDir, home))
				.isEqualTo(workingDir.resolve("profiles.properties"));
	}

	@Test
	void missingFileIsEmptyConfig() {
		ProfileManager.Config config = ProfileManager.load(tempDir.resolve("nope.properties"));
		assertThat(config.profiles()).isEmpty();
		assertThat(config.lastActive()).isNull();
	}

	@Test
	void parsesProfilesSortedByName() throws Exception {
		Path file = write("""
				last-active=invest
				profile.invest.data-dir=D:/cloud/matrosdms/invest
				profile.invest.repository-path=D:/cloud/matrosdms/repository/invest
				profile.invest.port=9091
				profile.household.data-dir=D:/cloud/matrosdms/household
				""");

		ProfileManager.Config config = ProfileManager.load(file);

		assertThat(config.lastActive()).isEqualTo("invest");
		assertThat(config.profiles()).extracting(ProfileManager.Profile::name)
				.containsExactly("household", "invest");

		ProfileManager.Profile household = config.profiles().get(0);
		assertThat(household.label()).isEqualTo("Household");
		assertThat(household.dataDir()).isEqualTo("D:/cloud/matrosdms/household");
		assertThat(household.repositoryPath()).isNull();
		assertThat(household.port()).isEqualTo(9090);

		ProfileManager.Profile invest = config.profiles().get(1);
		assertThat(invest.repositoryPath()).isEqualTo("D:/cloud/matrosdms/repository/invest");
		assertThat(invest.port()).isEqualTo(9091);
	}

	@Test
	void autoAssignsDistinctPortsSkippingExplicitOnes() throws Exception {
		Path file = write("""
				profile.aaa.data-dir=D:/data/aaa
				profile.bbb.data-dir=D:/data/bbb
				profile.bbb.port=9090
				profile.ccc.data-dir=D:/data/ccc
				""");

		List<ProfileManager.Profile> profiles = ProfileManager.load(file).profiles();

		// bbb pins 9090 explicitly, so aaa and ccc fill 9091 and 9092.
		assertThat(profiles).extracting(ProfileManager.Profile::name, ProfileManager.Profile::port)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("aaa", 9091),
						org.assertj.core.groups.Tuple.tuple("bbb", 9090),
						org.assertj.core.groups.Tuple.tuple("ccc", 9092));
	}

	@Test
	void ignoresEntriesWithoutDataDirAndBadPorts() throws Exception {
		Path file = write("""
				profile.broken.port=9091
				profile.ok.data-dir=D:/data/ok
				profile.ok.port=not-a-number
				profile..data-dir=D:/data/nameless
				""");

		ProfileManager.Config config = ProfileManager.load(file);

		assertThat(config.profiles()).extracting(ProfileManager.Profile::name).containsExactly("ok");
		// The unparseable port falls back to auto-assignment.
		assertThat(config.profiles().get(0).port()).isEqualTo(9090);
	}

	@Test
	void labelsArePrettifiedUnlessExplicit() throws Exception {
		Path file = write("""
				profile.demo-a.data-dir=D:/data/a
				profile.schwehla_household.data-dir=D:/data/h
				profile.custom.data-dir=D:/data/c
				profile.custom.label=My Fancy Tenant
				""");

		assertThat(ProfileManager.load(file).profiles())
				.extracting(ProfileManager.Profile::name, ProfileManager.Profile::label)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("custom", "My Fancy Tenant"),
						org.assertj.core.groups.Tuple.tuple("demo-a", "Demo A"),
						org.assertj.core.groups.Tuple.tuple("schwehla_household", "Schwehla Household"));
	}

	@Test
	void saveLastActiveReplacesLineAndKeepsComments() throws Exception {
		Path file = write("""
				# my tenants
				last-active=household
				profile.household.data-dir=D:/data/household
				profile.invest.data-dir=D:/data/invest
				""");

		ProfileManager.saveLastActive(file, "invest");

		List<String> lines = Files.readAllLines(file);
		assertThat(lines).contains("# my tenants", "last-active=invest");
		assertThat(lines).noneMatch(l -> l.equals("last-active=household"));
		assertThat(ProfileManager.load(file).lastActive()).isEqualTo("invest");
	}

	@Test
	void saveLastActivePrependsWhenAbsent() throws Exception {
		Path file = write("profile.household.data-dir=D:/data/household\n");

		ProfileManager.saveLastActive(file, "household");

		assertThat(Files.readAllLines(file).get(0)).isEqualTo("last-active=household");
		assertThat(ProfileManager.load(file).lastActive()).isEqualTo("household");
	}
}
