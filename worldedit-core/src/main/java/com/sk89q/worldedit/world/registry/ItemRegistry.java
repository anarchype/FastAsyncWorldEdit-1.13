/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.world.registry;

import com.sk89q.worldedit.world.item.ItemType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

public interface ItemRegistry {

    /**
     * Register all items
     */
    default Collection<String> registerItems() {
        return Collections.emptyList();
    }
    
    /**
     * Gets the name for the given item.
     *
     * @param itemType the item
     * @return The name, or null if it's unknown
     */
    @Nullable
    String getName(ItemType itemType);

}
