package com.epimorphismmc.eunetwork.integration.ftbteams;

import com.epimorphismmc.eunetwork.api.*;
import com.epimorphismmc.eunetwork.common.ServerEUNetwork;
import com.epimorphismmc.eunetwork.common.data.EUNetworkTypes;
import com.epimorphismmc.monomorphism.utility.MOUtils;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.epimorphismmc.eunetwork.api.EUNetValues.RESPONSE_REJECT;

public class FTBEUNetwork extends ServerEUNetwork {

    public static final IEUNetworkFactory<FTBEUNetwork> FACTORY = new IEUNetworkFactory<>() {

        @Override
        public FTBEUNetwork createEUNetwork(int id, String name, @NotNull Player owner) {
            return new FTBEUNetwork(id, name, owner);
        }

        @Override
        public FTBEUNetwork deserialize(CompoundTag tag, byte type) {
            var network = new FTBEUNetwork();
            network.deserialize(tag, type);
            return network;
        }

        @Override
        public String getType() {
            return EUNetworkTypes.FTB;
        }
    };

    public FTBEUNetwork() {
    }

    public FTBEUNetwork(int id, String name, @NotNull Player owner) {
        super(id, name, owner);
    }

    @NotNull
    @Override
    public Collection<NetworkMember> getAllMembers() {
        return getTeam()
                .map(Team::getMembers)
                .stream()
                .flatMap(Set::stream)
                .map(this::getMemberByUUID)
                .toList();
    }

    @Nullable
    @Override
    public NetworkMember getMemberByUUID(@NotNull UUID uuid) {
        var accessLevel = getPlayerAccess(uuid);
        if (accessLevel == AccessLevel.BLOCKED) return null;

        var player = MOUtils.getPlayerByUUID(uuid);
        if (player == null) return null;
        return NetworkMember.create(player, accessLevel);
    }

    @Override
    public boolean canPlayerAccess(@NotNull UUID uuid) {
        return getTeam()
                .map(team -> team.getRankForPlayer(uuid).isMemberOrBetter())
                .orElse(false);
    }

    @NotNull
    @Override
    public AccessLevel getPlayerAccess(@NotNull UUID uuid) {
        return getTeam()
                .map(team -> team.getRankForPlayer(uuid))
                .map(TeamUtils::rankToAccessLevel)
                .orElse(AccessLevel.BLOCKED);
    }

    @Override
    public int changeMembership(@NotNull Player player, @NotNull UUID targetUUID, byte type) {
        return RESPONSE_REJECT; // We delegate access management to FTB Teams
    }

    @Override
    public void serialize(@Nonnull CompoundTag tag, byte type) {
        if (type == EUNetValues.NBT_NET_BASIC || type == EUNetValues.NBT_SAVE_ALL) {
            tag.putInt(EUNetValues.NETWORK_ID, id);
            tag.putString(NETWORK_NAME, name);
            tag.putUUID(OWNER_UUID, owner);
            tag.putString(STORAGE, getStorage().toString());
        }

        if (type == EUNetValues.NBT_NET_STATISTICS) {
            mStatistics.writeNBT(tag);
        }
    }

    @Override
    public IEUNetworkFactory<? extends IEUNetwork> getFactory() {
        return FACTORY;
    }

    private Optional<Team> getTeam() {
        return getManager().getTeamByID(getOwner());
    }

    private TeamManager getManager() {
        return FTBTeamsAPI.api().getManager();
    }
}
