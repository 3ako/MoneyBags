package holy.moneybags.storage;

import lombok.Getter;

public class StorageManager {
    @Getter
    private final Storage storage;

    public StorageManager(Storage storage) {
        this.storage = storage;
    }
}

