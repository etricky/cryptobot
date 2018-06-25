package com.etricky.cryptobot.core.interfaces.jsonFiles;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingsJson {
	String cleanOldRecords;
	String cleanCron;
}
