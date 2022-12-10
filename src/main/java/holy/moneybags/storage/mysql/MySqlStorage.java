package holy.moneybags.storage.mysql;

import holy.moneybags.Moneybags;
import holy.moneybags.mbags.MBagsUser;
import holy.moneybags.mbags.items.DynamicItem;
import holy.moneybags.mbags.items.DynamicItemsManager;
import holy.moneybags.storage.Storage;
import holy.moneybags.storage.files.ConfigurationManager;
import org.bukkit.Material;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MySqlStorage implements Storage {
    private final MySqlManager mySqlManager;

    public MySqlStorage(){
        ConfigurationManager cm = Moneybags.getConfigurationManager();
        mySqlManager = new MySqlManager(cm);
    }

    public ArrayList<MBagsUser.ItemProgress> getUserStaticItemsProgress(final MBagsUser user, final Connection conn){
        final ArrayList<MBagsUser.ItemProgress> statistics = new ArrayList<>();
        try {
            final PreparedStatement statement = conn.prepareStatement("SELECT * FROM `"+mySqlManager.getBaseName()+
                    "`.`static_items_statistic` WHERE `uuid`=? LIMIT 7;");
            statement.setString(1, user.getUuid().toString());

            final ResultSet rs = statement.executeQuery();
            while (rs.next()){
                final   int    progress = rs.getInt("progress");
                final Material material = Material.getMaterial(rs.getString("material"));

                statistics.add(new MBagsUser.ItemProgress(material, progress));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return statistics;
    }
    @Override
    public ArrayList<MBagsUser.ItemProgress> getUserStaticItemsProgress(final MBagsUser user){
        final Connection conn = mySqlManager.getConnection();
        final ArrayList<MBagsUser.ItemProgress> statistics = getUserStaticItemsProgress(user, conn);
        mySqlManager.returnConnection(conn);
        return statistics;
    }
    @Override
    public MBagsUser getUser(final UUID uuid) {
        final Connection conn = mySqlManager.getConnection();
        try {
            final PreparedStatement statement = conn.prepareStatement("SELECT * FROM `"+mySqlManager.getBaseName()+
                    "`.`users` WHERE `uuid`=? LIMIT 1;");
            statement.setString(1, uuid.toString());

            final ResultSet rs = statement.executeQuery();
            if (!rs.next()) return null;

            final int lvl = rs.getInt("lvl");

            rs.close();

            final MBagsUser mbagsUser = new MBagsUser(uuid,lvl);

            if (mbagsUser.getLvl() < 11){
                // Load static progress
                for (final MBagsUser.ItemProgress progress: getUserStaticItemsProgress(mbagsUser,conn)) {
                    mbagsUser.addStaticProgress(progress,false);
                }
            }

            // Load dynamic items
            for (final MBagsUser.DynamicItemProgress item: getUserDynamicItems(uuid,conn)) {
                mbagsUser.setDynamicProgress(item,false);
            }

            return mbagsUser;
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            mySqlManager.returnConnection(conn);
        }

        return null;
    }

    @Override
    public MBagsUser createUser(final UUID uuid) {
        final Connection conn = mySqlManager.getConnection();
        try {
            PreparedStatement preparedStmt = conn.prepareStatement("INSERT INTO `"+mySqlManager.getBaseName()+
                    "`.`users` (uuid, lvl) VALUES (?,?)");
            preparedStmt.setString(1, uuid.toString());
            preparedStmt.setInt(2,1);

            preparedStmt.executeUpdate();

            preparedStmt.close();
            return new MBagsUser(uuid,1);
        } catch (SQLException e){
            e.printStackTrace();
        }
        finally {
            mySqlManager.returnConnection(conn);
        }
        return null;
    }

    @Override
    public void saveUser(final MBagsUser user) {
        final Connection conn = mySqlManager.getConnection();
        try {
            String request = "UPDATE `"+mySqlManager.getBaseName()+
                    "`.`users` SET `lvl` = ? WHERE `uuid` = ?";

            final PreparedStatement preparedStmt = conn.prepareStatement(request);
            preparedStmt.setInt(1, user.getLvl());
            preparedStmt.setString(2, user.getUuid().toString());
            preparedStmt.executeUpdate();
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            mySqlManager.returnConnection(conn);
        }
    }


    @Override
    public void createUserStaticStatistics(final UUID userUuid, final List<MBagsUser.ItemProgress> staticItems) {
        final Connection conn = mySqlManager.getConnection();
        try {
            String request = "INSERT INTO `"+mySqlManager.getBaseName()+
                    "`.`static_items_statistic` (uuid, material, progress) VALUES (?,?,?)";

        for (int i = 1; i < staticItems.size(); i++) {
            request += ",(?,?,?)";
        }
        PreparedStatement ps = conn.prepareStatement(request);
        for (int i = 0; i < staticItems.size(); i++) {
            final MBagsUser.ItemProgress progress = staticItems.get(i);
            ps.setString(i*3+1,userUuid.toString());
            ps.setString(i*3+2,progress.getMaterial().toString());
            ps.setInt(i*3+3,progress.getProgress());
        }

        ps.executeUpdate();

        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            mySqlManager.returnConnection(conn);
        }
    }

    @Override
    public void deleteUserStaticStatistics(UUID userUuid, List<MBagsUser.ItemProgress> staticItems) {
        final Connection conn = mySqlManager.getConnection();
        try {
            String request = "DELETE FROM `"+mySqlManager.getBaseName()+
                    "`.`static_items_statistic` WHERE (`uuid` = ? AND `material` = ?)";

            for (int i = 1; i < staticItems.size(); i++) {
                request += " OR (`uuid` = ? AND `material` = ?)";
            }
            PreparedStatement ps = conn.prepareStatement(request);

            for (int i = 0; i < staticItems.size(); i++) {
                final MBagsUser.ItemProgress progress = staticItems.get(i);
                ps.setString(i*2+1,userUuid.toString());
                ps.setString(i*2+2,progress.getMaterial().toString());
            }

            ps.executeUpdate();

        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            mySqlManager.returnConnection(conn);
        }
    }

    @Override
    public void updateUserStaticStatistics(UUID userUuid, List<MBagsUser.ItemProgress> staticItems) {
        final Connection conn = mySqlManager.getConnection();
        try {
            String request = "UPDATE `"+mySqlManager.getBaseName()+
                    "`.`static_items_statistic` SET `progress` = ? WHERE (`uuid` = ? AND `material` = ?)";

            for (final MBagsUser.ItemProgress progress : staticItems) {
                PreparedStatement ps = conn.prepareStatement(request);
                ps.setInt(1,progress.getProgress());
                ps.setString(2,userUuid.toString());
                ps.setString(3, progress.getMaterial().toString());
                ps.executeUpdate();
            }

        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            mySqlManager.returnConnection(conn);
        }
    }

    @Override
    public void deleteAllDynamicItems() {
        final Connection conn = mySqlManager.getConnection();
        try {
            Statement statement = conn.createStatement();
            statement.executeUpdate("DELETE FROM `"+mySqlManager.getBaseName()+ "`.`dynamic_items`");
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            mySqlManager.returnConnection(conn);
        }
    }

    @Override
    public List<MBagsUser.DynamicItemProgress> getUserDynamicItems(UUID uuid) {
        final Connection conn = mySqlManager.getConnection();
        final List<MBagsUser.DynamicItemProgress> items = getUserDynamicItems(uuid,conn);
        mySqlManager.returnConnection(conn);
        return items;
    }
    public List<MBagsUser.DynamicItemProgress> getUserDynamicItems(final UUID uuid, final Connection conn) {
        final List<MBagsUser.DynamicItemProgress> items = new ArrayList<>();
        try {
            final DynamicItemsManager dynamicItemsManager = Moneybags.getInstance().getDynamicItemsManager();
            final PreparedStatement statement = conn.prepareStatement("SELECT * FROM `"+mySqlManager.getBaseName()+
                    "`.`dynamic_items` WHERE `uuid`=?;");
            statement.setString(1, uuid.toString());

            final ResultSet rs = statement.executeQuery();

            while (rs.next()){
                final int   lvl      = rs.getInt("lvl");
                final int progress   = rs.getInt("progress");
                final Material material = Material.getMaterial(rs.getString("material").toUpperCase());

                final DynamicItem item  = dynamicItemsManager.getItem(material,lvl);
                if (item != null){
                    items.add(new MBagsUser.DynamicItemProgress(item,progress,item.getLvl()));
                }
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return items;
    }

    @Override
    public void createDynamicItems(Map<UUID, List<MBagsUser.DynamicItemProgress>> items) {
        final Connection conn = mySqlManager.getConnection();
        try {
            final String request = "INSERT INTO `"+mySqlManager.getBaseName()+
                    "`.`dynamic_items` (uuid, material, lvl, progress) VALUES (?,?,?,?)";

            for (final Map.Entry<UUID,List<MBagsUser.DynamicItemProgress>> entry: items.entrySet()){
                final UUID uuid = entry.getKey();
                for (final MBagsUser.DynamicItemProgress item : entry.getValue()){
                    final PreparedStatement ps = conn.prepareStatement(request);
                    ps.setString(1,uuid.toString());
                    ps.setString(2, item.getMaterial().toString());
                    ps.setInt(3, item.getLvl());
                    ps.setInt(4,item.getProgress());
                    ps.executeUpdate();
                    ps.close();
                }
            }

        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            mySqlManager.returnConnection(conn);
        }
    }

    @Override
    public void createDynamicItems(UUID uuid, List<MBagsUser.DynamicItemProgress> items) {
        final Connection conn = mySqlManager.getConnection();
        try {
            final String request = "INSERT INTO `"+mySqlManager.getBaseName()+
                    "`.`dynamic_items` (uuid, material, lvl, progress) VALUES (?,?,?,?)";

            for (final MBagsUser.DynamicItemProgress item : items){
                final PreparedStatement ps = conn.prepareStatement(request);
                ps.setString(1,uuid.toString());
                ps.setString(2, item.getMaterial().toString());
                ps.setInt(3, item.getLvl());
                ps.setInt(4, item.getProgress());
                ps.executeUpdate();
                ps.close();
            }

        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            mySqlManager.returnConnection(conn);
        }
    }

    @Override
    public void updateDynamicItems(UUID uuid, List<MBagsUser.DynamicItemProgress> items) {
        final Connection conn = mySqlManager.getConnection();
        try {
            String request = "UPDATE `"+mySqlManager.getBaseName()+
                    "`.`dynamic_items` SET `progress` = ? WHERE (`uuid` = ? AND `material` = ? AND `lvl` = ?)";

            for (final MBagsUser.DynamicItemProgress progress : items) {
                PreparedStatement ps = conn.prepareStatement(request);
                ps.setInt(1,progress.getProgress());
                ps.setString(2,uuid.toString());
                ps.setString(3, progress.getMaterial().toString());
                ps.setInt(4,progress.getLvl());
                ps.executeUpdate();
            }

        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            mySqlManager.returnConnection(conn);
        }
    }
}
