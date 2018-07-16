package com.etricky.cryptobot.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import com.etricky.cryptobot.model.TradesEntity;
import com.etricky.cryptobot.model.ExchangePK;

public interface TradesEntityRepository extends CrudRepository<TradesEntity, ExchangePK> {

	/**
	 * Returns the first and last entries in the database. If no data then returns 0
	 * and 0. If searched unixtime is the last value it will return the same value
	 * on both return parameters
	 * 
	 * @param exchange
	 * @param currency
	 * @param unixtime
	 * @return
	 */
	@Query(value = "select COALESCE(MIN(unixtime),0), COALESCE(MAX(unixtime),0) from TRADES where exchange=?1 and currency=?2 and unixtime >=?3", nativeQuery = true)
	List<Object[]> getFirstLastTrade(String exchange, String currency, Long unixtime);

	/**
	 * Returns all gaps in the middle of the data.
	 * 
	 * @param exchange
	 * @param currency
	 * @param startDataUnixTime
	 * @param endDataUnixTime
	 * @return
	 */
	@Query(value = "SELECT start_gap, (SELECT MIN(UNIXTIME) FROM TRADES WHERE EXCHANGE = ?1 AND CURRENCY = ?2 AND UNIXTIME > start_gap) AS end_gap "
			+ "FROM (SELECT UNIXTIME + 60 AS start_gap FROM TRADES t1 WHERE EXCHANGE = ?1 AND CURRENCY = ?2 AND UNIXTIME >= ?3 AND UNIXTIME < ?4 AND NOT EXISTS (SELECT NULL "
			+ "FROM TRADES t2 WHERE t1.UNIXTIME + 60 = t2.UNIXTIME AND EXCHANGE = ?1 AND CURRENCY = ?2 AND UNIXTIME >= ?3)) AS trades2 ORDER BY start_gap ASC", nativeQuery = true)
	Optional<List<Object[]>> getGaps(String exchange, String currency, long startDataUnixTime, long endDataUnixTime);

	/**
	 * Deletes all records that are older than unixTime
	 * 
	 * @param unixTime
	 * @return Number of records deleted
	 */
	@Modifying
	@Transactional
	@Query(value = "DELETE FROM TRADES WHERE UNIXTIME < ?1", nativeQuery = true)
	int deleteOldRecords(long unixTime);

	/**
	 * Returns all trades that are not fake in the specified period
	 * 
	 * @param exchange
	 * @param currency
	 * @param startDataUnixTime
	 * @param endDataUnixTime
	 * @return
	 */
	@Query(value = "SELECT * FROM TRADES t1 WHERE EXCHANGE = ?1 AND CURRENCY = ?2 AND UNIXTIME >= ?3 AND UNIXTIME <= ?4 AND FAKE_TRADE = FALSE", nativeQuery = true)
	List<TradesEntity> getTradesInPeriodNoFake(String exchange, String currency, long startDataUnixTime, long endDataUnixTime);

	/**
	 * Returns all trades, fake or not, in the specified period
	 * 
	 * @param exchange
	 * @param currency
	 * @param startDataUnixTime
	 * @param endDataUnixTime
	 * @return
	 */
	@Query(value = "SELECT * FROM TRADES t1 WHERE EXCHANGE = ?1 AND CURRENCY = ?2 AND UNIXTIME >= ?3 AND UNIXTIME <= ?4", nativeQuery = true)
	List<TradesEntity> getAllTradesInPeriod(String exchange, String currency, long startDataUnixTime, long endDataUnixTime);
}