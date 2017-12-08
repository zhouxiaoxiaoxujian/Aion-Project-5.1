package mysql5;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.dao.PlayerPassportsDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.PlayerPassports;
import com.aionemu.gameserver.model.templates.event.AtreianPassport;
import com.aionemu.gameserver.services.AtreianPassportService;

/**
*
* @author Ranastic
*/
public class MySQL5PlayerPassportsDAO extends PlayerPassportsDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL5PlayerPassportsDAO.class);
    public static final String SELECT_QUERY = "SELECT `passportid`, `rewarded` FROM `player_passports` WHERE `player_id`=?";
    public static final String UPDATE_QUERY = "UPDATE `player_passports` SET `rewarded`=? WHERE `player_id`=? AND `passportid`=?";
    public static final String INSERT_QUERY = "INSERT INTO `player_passports` (`player_id`, `passportid`, `rewarded`) VALUES (?,?,?)";

    /* (non-Javadoc)
     * @see com.aionemu.commons.database.dao.DAO#supports(java.lang.String, int, int)
     */
    @Override
    public boolean supports(String s, int i, int i1) {
        return MySQL5DAOUtils.supports(s, i, i1);
    }

    /* (non-Javadoc)
     * @see com.aionemu.gameserver.dao.PlayerPassportsDAO#load(com.aionemu.gameserver.model.gameobjects.player.Player)
     */
    @Override
    public PlayerPassports load(Player player) {
        PlayerPassports pp = new PlayerPassports();

        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = DatabaseFactory.getConnection();
            stmt = con.prepareStatement(SELECT_QUERY);
            stmt.setInt(1, player.getObjectId());
            ResultSet rset = stmt.executeQuery();
            while (rset.next()) {
                int passportid = rset.getInt("passportid");
                int rewarded = rset.getInt("rewarded");
                AtreianPassport atp = AtreianPassportService.getInstance().data.get(passportid);
                if (rewarded == 0)
                    atp.setRewardId(1);
                else
                    atp.setRewardId(3);
                pp.addPassport(atp.getId(), atp);
            }
            rset.close();
        } catch (Exception e) {
            log.error(
                    "Could not restore completed passport data for player: " + player.getObjectId() + " from DB: " + e.getMessage(), e);
        } finally {
            DatabaseFactory.close(stmt, con);
        }
        return pp;
    }

    /* (non-Javadoc)
     * @see com.aionemu.gameserver.dao.PlayerPassportsDAO#store(com.aionemu.gameserver.model.gameobjects.player.Player)
     */
    @Override
    public void store(Player player) {

        Collection<AtreianPassport> pList = player.getCommonData().getCompletedPassports().getAllPassports();
        if (GenericValidator.isBlankOrNull(pList)) {
            return;
        }

        Connection con = null;
        try {
            con = DatabaseFactory.getConnection();
            con.setAutoCommit(false);
            try {
                addPassports(con, player.getObjectId(), pList, player.getCommonData());
            } catch (@SuppressWarnings("unused") Exception e) {
                updatePassports(con, player.getObjectId(), pList, player.getCommonData());
            }
        } catch (SQLException e) {
            log.error("Can't save passports for player " + player.getObjectId(), e);
        } finally {
            DatabaseFactory.close(con);
        }
    }

    private void addPassports(Connection con, int playerId, Collection<AtreianPassport> atp, PlayerCommonData pcd) {

        if (GenericValidator.isBlankOrNull(atp)) {
            return;
        }

        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(INSERT_QUERY);

            for (AtreianPassport pp : atp) {
                ps.setInt(1, playerId);
                ps.setInt(2, pp.getId());
                ps.setInt(3, pcd.getPassportReward());
                ps.addBatch();
            }

            ps.executeBatch();
            con.commit();
        } catch (@SuppressWarnings("unused") SQLException e) {
        } finally {
            DatabaseFactory.close(ps);
        }
    }

    private void updatePassports(Connection con, int playerId, Collection<AtreianPassport> atp, PlayerCommonData pcd) {

        if (GenericValidator.isBlankOrNull(atp)) {
            return;
        }

        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(UPDATE_QUERY);

            for (AtreianPassport pp : atp) {
                ps.setInt(1, pcd.getPassportReward());
                ps.setInt(2, playerId);
                ps.setInt(3, pp.getId());
                ps.addBatch();
            }

            ps.executeBatch();
            con.commit();
        } catch (@SuppressWarnings("unused") SQLException e) {
            log.error("Failed to update existing passports for player " + playerId);
        } finally {
            DatabaseFactory.close(ps);
        }
    }

}
