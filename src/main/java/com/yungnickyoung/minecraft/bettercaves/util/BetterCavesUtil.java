package com.yungnickyoung.minecraft.bettercaves.util;

import com.yungnickyoung.minecraft.bettercaves.config.Configuration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSand;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

/**
 * Utility functions for BetterCaves. These functions are mostly refactored versions of methods found in
 * {@code net.minecraft.world.gen.MapGenCaves}.
 * This class may not be instantiated - all members are {@code public} and {@code static},
 * and as such may be accessed freely.
 */
public class BetterCavesUtil {
    private BetterCavesUtil() {} // Private constructor prevents instantiation

    /* Common IBlockStates used in this class */
    private static final IBlockState BLOCKSTATE_AIR = Blocks.AIR.getDefaultState();
    private static final IBlockState BLOCKSTATE_LAVA = Blocks.LAVA.getDefaultState();
    private static final IBlockState BLOCKSTATE_SAND = Blocks.SAND.getDefaultState();
    private static final IBlockState BLOCKSTATE_SANDSTONE = Blocks.SANDSTONE.getDefaultState();
    private static final IBlockState BLOCKSTATE_REDSANDSTONE = Blocks.RED_SANDSTONE.getDefaultState();

    /**
     * Determine if the block at the specified location is the designated top block for the biome.
     *
     * @param world the Minecraft world this block is in
     * @param primer the ChunkPrimer containing the block
     * @param localX the block's chunk-local x-coordinate
     * @param y the block's chunk-local y-coordinate (same as real y-coordinate)
     * @param localZ the block's chunk-local z-coordinate
     * @param chunkX the chunk's x coordinate
     * @param chunkZ the chunk's z coordinate
     * @return true if this block is the same type as the biome's designated top block
     */
    public static boolean isTopBlock(World world, ChunkPrimer primer, int localX, int y, int localZ, int chunkX, int chunkZ) {
        int realX = (chunkX * 16) + localX;
        int realZ = (chunkZ * 16) + localZ;
        Biome biome = world.getBiome(new BlockPos(realX, y, realZ));
        IBlockState blockState = primer.getBlockState(localX, y, localZ);
        return blockState == biome.topBlock;
    }

    /**
     * Digs out the current block, default implementation removes stone, filler, and top block.
     * Sets the block to lavaBlockState if y is less then the lavaDepth in the Config, and air other wise.
     * If setting to air, it also checks to see if we've broken the surface, and if so,
     * tries to make the floor the biome's top block.
     *
     * @param world the Minecraft world this block is in
     * @param primer the ChunkPrimer containing the block
     * @param lavaBlockState the BlockState to use as lava. If you want regular lava, you can either specify it, or
     *                       use the wrapper function without this param
     * @param localX the block's chunk-local x coordinate
     * @param y the block's chunk-local y coordinate (same as real y-coordinate)
     * @param localZ the block's chunk-local z coordinate
     * @param chunkX the chunk's x coordinate
     * @param chunkZ the chunk's z coordinate
     */
    public static void digBlock(World world, ChunkPrimer primer, IBlockState lavaBlockState, int localX, int y, int localZ, int chunkX, int chunkZ) {
        IBlockState blockState = primer.getBlockState(localX, y, localZ);
        IBlockState blockStateAbove = primer.getBlockState(localX, y + 1, localZ);

        int realX = (chunkX * 16) + localX;
        int realZ = (chunkZ * 16) + localZ;

        Biome biome = world.getBiome(new BlockPos(realX, y, realZ));
        Block biomeTopBlock = biome.topBlock.getBlock();
        Block biomeFillerBlock = biome.fillerBlock.getBlock();

        // Only continue if the block is replaceable
        if (canReplaceBlock(blockState, blockStateAbove) || blockState.getBlock() == biomeTopBlock || blockState.getBlock() == biomeFillerBlock) {
            if (y <= Configuration.lavaDepth) { // Replace any block below the lava depth with the lava block passed in
                primer.setBlockState(localX, y, localZ, lavaBlockState);
            } else {
                // Adjust block below if block removed is biome top block
                if (isTopBlock(world, primer, localX, y, localZ, chunkX, chunkZ) && canReplaceBlock(primer.getBlockState(localX, y - 1, localZ), BLOCKSTATE_AIR))
                    primer.setBlockState(localX, y - 1, localZ, biome.topBlock);

                // Replace this block with air, effectively "digging" it out
                primer.setBlockState(localX, y, localZ, BLOCKSTATE_AIR);

                // If we caused floating sand to form, replace it with sandstone
                if (blockStateAbove == BLOCKSTATE_SAND)
                    primer.setBlockState(localX, y + 1, localZ, BLOCKSTATE_SANDSTONE);
                else if (blockStateAbove == BLOCKSTATE_SAND.withProperty(BlockSand.VARIANT, BlockSand.EnumType.RED_SAND))
                    primer.setBlockState(localX, y + 1, localZ, BLOCKSTATE_REDSANDSTONE);
            }
        }
    }

    /**
     * Wrapper function for digBlock with default lava block.
     * Digs out the current block, default implementation removes stone, filler, and top block.
     * Sets the block to lavaBlockState if y is less then the lavaDepth in the Config, and air other wise.
     * If setting to air, it also checks to see if we've broken the surface, and if so,
     * tries to make the floor the biome's top block.
     *
     * @param world the Minecraft world this block is in
     * @param primer the ChunkPrimer containing the block
     * @param localX the block's chunk-local x coordinate
     * @param y the block's chunk-local y coordinate (same as real y-coordinate)
     * @param localZ the block's chunk-local z coordinate
     * @param chunkX the chunk's x coordinate
     * @param chunkZ the chunk's z coordinate
     */
    public static void digBlock(World world, ChunkPrimer primer, int localX, int y, int localZ, int chunkX, int chunkZ) {
        digBlock(world, primer, BLOCKSTATE_LAVA, localX, y, localZ, chunkX, chunkZ);
    }

    /**
     * Determines if the Block of a given IBlockState is suitable to be replaced during cave generation.
     * Basically returns true for most common worldgen blocks (e.g. stone, dirt, sand), false if the block is air.
     *
     * @param blockState the block's IBlockState
     * @param blockStateAbove the IBlockState of the block above this one
     * @return true if the blockState can be replaced
     */
    public static boolean canReplaceBlock(IBlockState blockState, IBlockState blockStateAbove) {
        Block block = blockState.getBlock();

        // Avoid damaging trees
        if (block == Blocks.LEAVES
                || block == Blocks.LEAVES2
                || block == Blocks.LOG
                || block == Blocks.LOG2)
            return false;

        // Avoid digging out under trees
        if (blockStateAbove == Blocks.LOG.getDefaultState()
                || blockStateAbove == Blocks.LOG2.getDefaultState())
            return false;

        // Don't mine bedrock
        if (blockState == Blocks.BEDROCK.getDefaultState())
            return false;

        // Accept stone-like blocks added from other mods
        if (blockState.getMaterial() == Material.ROCK)
            return true;

        // Mine-able blocks
        if (block == Blocks.STONE
                || block == Blocks.DIRT
                || block == Blocks.GRASS
                || block == Blocks.HARDENED_CLAY
                || block == Blocks.STAINED_HARDENED_CLAY
                || block == Blocks.SANDSTONE
                || block == Blocks.RED_SANDSTONE
                || block == Blocks.MYCELIUM
                || block  == Blocks.SNOW_LAYER)
            return true;

        // Only accept gravel and sand if water is not directly above it
        return (block == Blocks.SAND || block == Blocks.GRAVEL)
                    && blockStateAbove.getMaterial() != Material.WATER;
    }

    /**
     * Tests 8 edge points and center of chunk to approximate max surface altitude (y-coordinate) of the chunk.
     * Note that water blocks also count as the surface.
     * @param primer primer for chunk
     * @return y-coordinate of the approximate highest surface altitude in the chunk
     */
    public static int getMaxSurfaceAltitudeChunk(ChunkPrimer primer) {
        int maxHeight = 0;
        int[] testCoords = {0, 7, 15}; // chunk-local x/z coordinates to test for max height

        for (int x : testCoords)
            for (int z : testCoords)
                maxHeight = Math.max(maxHeight, getSurfaceAltitudeForColumn(primer, x, z));

        return maxHeight;
    }

    /**
     * Tests 8 edge points and center of chunk to approximate min surface altitude (y-coordinate) of the chunk.
     * Note that water blocks also count as the surface.
     * @param primer primer for chunk
     * @return y-coordinate of the approximate lowest surface altitude in the chunk
     */
    public static int getMinSurfaceAltitudeChunk(ChunkPrimer primer) {
        int minHeight = 256;
        int[] testCoords = {0, 7, 15}; // chunk-local x/z coordinates to test for max height

        for (int x : testCoords)
            for (int z : testCoords)
                minHeight = Math.min(minHeight, getSurfaceAltitudeForColumn(primer, x, z));

        return minHeight;
    }

    /**
     * Tests every block in a 2x2 "sub-chunk" to get the max surface altitude (y-coordinate) of the sub-chunk.
     * Note that water blocks also count as the surface.
     * @param primer primer for chunk
     * @param subX The x-coordinate of the sub-chunk. Note that this is regular chunk-local x-coordinate divided
     *             by 2. E.g. If you want the last 2 blocks on the x-axis in the chunk (blocks 14 and 15), use subX = 7.
     * @param subZ The z-coordinate of the sub-chunk. Note that this is regular chunk-local z-coordinate divided
     *             by 2. E.g. If you want the last 2 blocks on the z-axis in the chunk (blocks 14 and 15), use subZ = 7.
     * @return Max surface height of the sub-chunk
     */
    public static int getMaxSurfaceAltitudeSubChunk(ChunkPrimer primer, int subX, int subZ)  {
        int maxHeight = 0;
        int[] testCoords = {0, 1}; // chunk-local x/z coordinates to test for max height

        for (int x : testCoords)
            for (int z : testCoords)
                maxHeight = Math.max(maxHeight, getSurfaceAltitudeForColumn(primer, (subX * 2) + x, (subZ * 2) + z));

        return maxHeight;
    }

    /**
     * Returns the y-coordinate of the surface block for a given local block coordinate for a given chunk.
     * Note that water blocks also count as the surface.
     * @param primer primer for chunk
     * @param localX The block's chunk-local x-coordinate
     * @param localZ The block's chunk-local z-coordinate
     * @return The y-coordinate of the surface block
     */
    public static int getSurfaceAltitudeForColumn(ChunkPrimer primer, int localX, int localZ) {
        return searchSurfaceAltitudeInRangeForColumn(primer, localX, localZ, 255, 0);
    }

    /**
     * Searches for the y-coordinate of the surface block for a given local block coordinate for a given chunk in a
     * specific range of y-coordinates.
     * Note that water blocks also count as the surface.
     * @param primer primer for chunk
     * @param localX The block's chunk-local x-coordinate
     * @param localZ The block's chunk-local z-coordinate
     * @param topY The top y-coordinate to stop searching at
     * @param bottomY The bottom y-coordinate to start searching at
     * @return The y-coordinate of the surface block
     */
    public static int searchSurfaceAltitudeInRangeForColumn(ChunkPrimer primer, int localX, int localZ, int topY, int bottomY) {
        // Edge case: blocks go all the way up to build height
        if (topY == 255
                && primer.getBlockState(localX, 255, localZ) != Blocks.AIR.getDefaultState()
                && primer.getBlockState(localX, 255, localZ).getMaterial() != Material.WATER)
            return 255;

        for (int y = bottomY; y <= topY; y++) {
            if (primer.getBlockState(localX, y, localZ) == Blocks.AIR.getDefaultState() || primer.getBlockState(localX, y, localZ).getMaterial() == Material.WATER)
                return y;
        }

        return -1; // Surface somehow not found
    }
}