/**
 * This file is part of FoamFixAPI.
 *
 * FoamFixAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoamFixAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FoamFixAPI.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with the Minecraft game engine, the Mojang Launchwrapper,
 * the Mojang AuthLib and the Minecraft Realms library (and/or modified
 * versions of said software), containing parts covered by the terms of
 * their respective licenses, the licensors of this Program grant you
 * additional permission to convey the resulting work.
 */
package pl.asie.foamfix.client;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMaps;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import pl.asie.foamfix.FoamFix;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import pl.asie.foamfix.ProxyClient;
import pl.asie.foamfix.shared.FoamFixShared;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FoamFixModelDeduplicate {
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onModelBake(ModelBakeEvent event) {
        // TODO: analyze impact + obj. references
        // TODO: figure out why it breaks Botania (#1, refer to vazkii/botania/client/model/FloatingFlowerModel.java)
        // FoamUtils.wipeModelLoaderRegistryCache();

        if (FoamFixShared.config.clDeduplicate) {
            ProgressManager.ProgressBar bakeBar = ProgressManager.push("FoamFix: deduplicating", event.getModelRegistry().getKeys().size());

            if (ProxyClient.deduplicator == null) {
                ProxyClient.deduplicator = new Deduplicator();
            }

            FoamFix.logger.info("Deduplicating models...");
            ProxyClient.deduplicator.maxRecursion = FoamFixShared.config.clDeduplicateRecursionLevel;

            ProxyClient.deduplicator.addObjects(Block.REGISTRY.getKeys());
            ProxyClient.deduplicator.addObjects(Item.REGISTRY.getKeys());

            for (ModelResourceLocation loc : event.getModelRegistry().getKeys()) {
                IBakedModel model = event.getModelRegistry().getObject(loc);
                String modelName = loc.toString();
                bakeBar.step(String.format("[%s]", modelName));
                try {
                    ProxyClient.deduplicator.addObject(loc);
                    ProxyClient.deduplicator.deduplicateObject(model, 0);
                } catch (Exception e) {

                }
            }

            ProgressManager.pop(bakeBar);
            FoamFix.logger.info("Deduplicated " + ProxyClient.deduplicator.successfuls + " objects.");
            /* List<Class> map = Lists.newArrayList(ProxyClient.deduplicator.dedupObjDataMap.keySet());
            map.sort(Comparator.comparingInt(a -> ProxyClient.deduplicator.dedupObjDataMap.get(a)));
            for (Class c : map) {
                FoamFix.logger.info(c.getSimpleName() + " = " + ProxyClient.deduplicator.dedupObjDataMap.get(c));
            } */
        }

        ProxyClient.deduplicator = null; // release deduplicator to save memory
        FoamFix.updateRamSaved();
    }
}
