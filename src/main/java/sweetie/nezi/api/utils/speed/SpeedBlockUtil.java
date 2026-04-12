package sweetie.nezi.api.utils.speed;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class SpeedBlockUtil {

    public static List<BlockPos> getCube(final BlockPos center, final int radiusXZ, final int radiusY) {
        List<BlockPos> positions = new ArrayList<>();

        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();

        for (int x = centerX - radiusXZ; x <= centerX + radiusXZ; x++) {
            for (int z = centerZ - radiusXZ; z <= centerZ + radiusXZ; z++) {
                for (int y = centerY - radiusY; y <= centerY + radiusY; y++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }

        return positions;
    }
}
