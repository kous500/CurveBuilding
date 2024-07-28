package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import me.kous500.curvebuilding.math.BlockVector3;

import static me.kous500.curvebuilding.WorldeditAdapter.adapt;

/**
 * 選択範囲内にあるブロックの情報を３次元配列として保存する
 */
public class RegionBlocks {
    private final int minX;
    private final int minY;
    private final int minZ;
    private final BaseBlock[][][] regionBlocks;

    public RegionBlocks(EditSession editSession, Region region) {
        BlockVector3 maxRegion = adapt(region.getMaximumPoint());
        BlockVector3 minRegion = adapt(region.getMinimumPoint());

        this.minX = minRegion.x();
        this.minY = minRegion.y();
        this.minZ = minRegion.z();

        this.regionBlocks = new BaseBlock[region.getWidth()][region.getHeight()][region.getLength()];

        for (int x = minX; x <= maxRegion.x(); x++) {
            for (int y = minY; y <= maxRegion.y(); y++) {
                for (int z = minZ; z <= maxRegion.z(); z++) {
                    regionBlocks[x - minX][y - minY][z - minZ] = editSession.getFullBlock(adapt(BlockVector3.at(x, y, z)));
                }
            }
        }
    }

    /**
     * 選択範囲の指定した座標のブロックを取得する
     * @param vec ブロックの座標
     * @return ブロックの情報
     */
    public BaseBlock get(BlockVector3 vec) {
        try {
            return regionBlocks[vec.x() - minX][vec.y() - minY][vec.z() - minZ];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }
}
