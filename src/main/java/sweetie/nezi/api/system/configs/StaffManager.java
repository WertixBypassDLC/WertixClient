package sweetie.nezi.api.system.configs;

import lombok.Getter;
import sweetie.nezi.api.system.files.AbstractFile;

public class StaffManager extends AbstractFile {
    @Getter private static final StaffManager instance = new StaffManager();

    @Override
    public String fileName() {
        return "staffs";
    }
}
