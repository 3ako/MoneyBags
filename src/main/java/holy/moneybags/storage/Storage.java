package holy.moneybags.storage;

import holy.moneybags.gui.mbags.MBagsUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface Storage {
    MBagsUser getUser(UUID uuid);
    MBagsUser createUser(UUID uuid);

    void saveUser(MBagsUser user);
    ArrayList<MBagsUser.ItemProgress> getUserStaticItemsProgress(final MBagsUser user);

    void createUserStaticStatistics(final UUID userUuid, final List<MBagsUser.ItemProgress> staticItems);
    void deleteUserStaticStatistics(final UUID userUuid, final List<MBagsUser.ItemProgress> staticItems);
    void updateUserStaticStatistics(final UUID userUuid, final List<MBagsUser.ItemProgress> staticItems);
    void deleteAllDynamicItems();
    List<MBagsUser.DynamicItemProgress> getUserDynamicItems(final UUID uuid);
    void createDynamicItems(final Map<UUID,List<MBagsUser.DynamicItemProgress>> items);
    void createDynamicItems(final UUID uuid, final List<MBagsUser.DynamicItemProgress> items);
    void updateDynamicItems(final UUID uuid, final List<MBagsUser.DynamicItemProgress> items);
}
