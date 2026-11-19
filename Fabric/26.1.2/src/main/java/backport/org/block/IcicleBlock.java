package backport.org.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.material.Fluids;

public final class IcicleBlock extends PointedDripstoneBlock {
   public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;

   public IcicleBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(ATTACHED, false));
   }

   public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double distance) {
      if (state.getValue(TIP_DIRECTION) == Direction.UP && state.getValue(THICKNESS) == tipThickness()) {
         entity.causeFallDamage(distance + 2.5, 2.0F, level.damageSources().stalagmite());
      } else {
         super.fallOn(level, state, pos, entity, distance);
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{ATTACHED});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState vanilla = super.getStateForPlacement(context);
      if (vanilla != null) {
         return this.updateAttachment(vanilla, context.getLevel(), context.getClickedPos());
      }

      BlockPos pos = context.getClickedPos();
      Direction preferred = context.getNearestLookingVerticalDirection().getOpposite();
      Direction direction = this.hasIcicleSupport(context.getLevel(), pos, preferred)
         ? preferred
         : (this.hasIcicleSupport(context.getLevel(), pos, preferred.getOpposite()) ? preferred.getOpposite() : null);
      return direction == null
         ? null
         : this.updateAttachment(
            (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(TIP_DIRECTION, direction)).setValue(THICKNESS, tipThickness()))
               .setValue(WATERLOGGED, context.getLevel().getFluidState(pos).getType() == Fluids.WATER),
            context.getLevel(),
            pos
         );
   }

   public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      Direction direction = (Direction)state.getValue(TIP_DIRECTION);
      BlockPos supportPos = pos.relative(direction.getOpposite());
      BlockState support = level.getBlockState(supportPos);
      return this.isIcicleWithDirection(support, direction) || support.isFaceSturdy(level, supportPos, direction);
   }

   protected BlockState updateShape(
      BlockState state,
      LevelReader level,
      ScheduledTickAccess ticks,
      BlockPos pos,
      Direction changedDirection,
      BlockPos changedPos,
      BlockState changedState,
      RandomSource random
   ) {
      if (changedDirection == ((Direction)state.getValue(TIP_DIRECTION)).getOpposite() && !this.canSurvive(state, level, pos)) {
         ticks.scheduleTick(pos, this, 2);
         return state;
      } else {
         BlockState result = super.updateShape(state, level, ticks, pos, changedDirection, changedPos, changedState, random);
         if (!result.is(this)) {
            return result;
         } else {
            return changedDirection.getAxis() == Axis.Y ? this.updateAttachment(this.updateThickness(result, level, pos), level, pos) : result;
         }
      }
   }

   private boolean hasIcicleSupport(LevelReader level, BlockPos pos, Direction direction) {
      BlockState support = level.getBlockState(pos.relative(direction.getOpposite()));
      return this.isIcicleWithDirection(support, direction);
   }

   private boolean isIcicleWithDirection(BlockState state, Direction direction) {
      return state.is(this) && state.getValue(TIP_DIRECTION) == direction;
   }

   private BlockState updateThickness(BlockState state, LevelReader level, BlockPos pos) {
      Direction direction = (Direction)state.getValue(TIP_DIRECTION);
      BlockState towardTip = level.getBlockState(pos.relative(direction));
      if (!this.isIcicleWithDirection(towardTip, direction)) {
         return this.isIcicleWithDirection(towardTip, direction.getOpposite())
            ? (BlockState)state.setValue(THICKNESS, mergedTipThickness())
            : (BlockState)state.setValue(THICKNESS, tipThickness());
      }

      if (isTip(towardTip)) {
         return (BlockState)state.setValue(THICKNESS, frustumThickness());
      }

      BlockState towardRoot = level.getBlockState(pos.relative(direction.getOpposite()));
      return (BlockState)state.setValue(THICKNESS, this.isIcicleWithDirection(towardRoot, direction) ? middleThickness() : baseThickness());
   }

   private BlockState updateAttachment(BlockState state, LevelReader level, BlockPos pos) {
      Direction direction = (Direction)state.getValue(TIP_DIRECTION);
      BlockState support = level.getBlockState(pos.relative(direction.getOpposite()));
      return (BlockState)state.setValue(ATTACHED, !this.isIcicleWithDirection(support, direction));
   }

   private static boolean isTip(BlockState state) {
      return state.getValue(THICKNESS) == tipThickness() || state.getValue(THICKNESS) == mergedTipThickness();
   }

   private static DripstoneThickness tipThickness() {
      return DripstoneThickness.TIP;
   }

   private static DripstoneThickness mergedTipThickness() {
      return DripstoneThickness.TIP_MERGE;
   }

   private static DripstoneThickness frustumThickness() {
      return DripstoneThickness.FRUSTUM;
   }

   private static DripstoneThickness middleThickness() {
      return DripstoneThickness.MIDDLE;
   }

   private static DripstoneThickness baseThickness() {
      return DripstoneThickness.BASE;
   }

   public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (!(Boolean)state.getValue(ATTACHED) && this.wouldMelt(level, pos)) {
         Direction direction = (Direction)state.getValue(TIP_DIRECTION);
         if (direction != Direction.UP && (!isTip(state) || level.getBlockState(pos.below()).isAir())) {
            this.spawnFallingColumn(level, pos);
         } else {
            level.destroyBlock(pos, true);
         }
      } else {
         if (random.nextFloat() < 0.011377778F
            && (Boolean)state.getValue(ATTACHED)
            && state.getValue(TIP_DIRECTION) == Direction.DOWN
            && level.getBlockState(pos.above()).is(Blocks.PACKED_ICE)
            && !this.wouldMelt(level, pos)) {
            this.growDownward(level, pos);
         }
      }
   }

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (state.getValue(TIP_DIRECTION) == Direction.DOWN && isTip(state) && level.getBlockState(pos.below()).isAir() && random.nextFloat() <= 0.02F) {
         level.addParticle(
            new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SNOW.defaultBlockState()),
            pos.getX() + 0.5,
            pos.getY() - 0.05,
            pos.getZ() + 0.5,
            0.0,
            0.0,
            0.0
         );
      }
   }

   private void growDownward(ServerLevel level, BlockPos root) {
      BlockPos tip = root;

      int length;
      for (length = 1; length < 5; length++) {
         BlockPos next = tip.below();
         BlockState nextState = level.getBlockState(next);
         if (!this.isIcicleWithDirection(nextState, Direction.DOWN)) {
            break;
         }

         tip = next;
      }

      if (length < 5 && level.getBlockState(tip.below()).isAir()) {
         level.setBlock(
            tip.below(),
            (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(TIP_DIRECTION, Direction.DOWN)).setValue(THICKNESS, tipThickness()))
               .setValue(ATTACHED, false),
            3
         );

         for (int index = 0; index <= length; index++) {
            BlockPos segmentPos = root.below(index);
            BlockState segment = level.getBlockState(segmentPos);
            if (segment.is(this)) {
               level.setBlock(segmentPos, this.updateAttachment(this.updateThickness(segment, level, segmentPos), level, segmentPos), 3);
            }
         }
      }
   }

   public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (!this.canSurvive(state, level, pos)) {
         if (state.getValue(TIP_DIRECTION) == Direction.DOWN) {
            this.spawnFallingColumn(level, pos);
         } else {
            level.destroyBlock(pos, true);
         }
      } else {
         super.tick(state, level, pos, random);
      }
   }

   private boolean wouldMelt(LevelReader level, BlockPos pos) {
      return level instanceof Level world && world.dimension() == Level.NETHER
         || level.getBrightness(LightLayer.BLOCK, pos) > 4
         || level.getBlockState(pos.below()).is(Blocks.MAGMA_BLOCK)
         || level.getBlockState(pos.below(2)).is(Blocks.MAGMA_BLOCK);
   }

   private void spawnFallingColumn(ServerLevel level, BlockPos start) {
      MutableBlockPos cursor = start.mutable();

      while (true) {
         BlockState segment = level.getBlockState(cursor);
         if (!this.isIcicleWithDirection(segment, Direction.DOWN)) {
            break;
         }

         FallingBlockEntity falling = FallingBlockEntity.fall(level, cursor, (BlockState)segment.setValue(ATTACHED, false));
         if (isTip(segment)) {
            falling.setHurtsEntities(Math.max(1 + start.getY() - cursor.getY(), 6), 40);
            break;
         }

         cursor.move(Direction.DOWN);
      }
   }
}
