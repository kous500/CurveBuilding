package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;

public class RegionBlocks {
    private final int minX;
    private final int minY;
    private final int minZ;
    private final BaseBlock[][][] regionBlocks;

    public RegionBlocks(EditSession editSession, Region region) {
        BlockVector3 maxRegion = region.getMaximumPoint();
        BlockVector3 minRegion = region.getMinimumPoint();

        this.minX = minRegion.getX();
        this.minY = minRegion.getY();
        this.minZ = minRegion.getZ();

        this.regionBlocks = new BaseBlock[region.getWidth()][region.getHeight()][region.getLength()];

        for (int x = minX; x <= maxRegion.getX(); x++) {
            for (int y = minY; y <= maxRegion.getY(); y++) {
                for (int z = minZ; z <= maxRegion.getZ(); z++) {
                    regionBlocks[x - minX][y - minY][z - minZ] = editSession.getFullBlock(BlockVector3.at(x, y, z));
                }
            }
        }
    }

    public BaseBlock get(BlockVector3 vec) {
        try {
            return regionBlocks[vec.getX() - minX][vec.getY() - minY][vec.getZ() - minZ];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }
}
