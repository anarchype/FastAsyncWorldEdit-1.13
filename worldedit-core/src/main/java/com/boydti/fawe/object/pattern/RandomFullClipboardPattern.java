package com.boydti.fawe.object.pattern;

import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.schematic.Schematic;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import java.io.IOException;
import java.io.NotSerializableException;
import java.util.List;


import static com.google.common.base.Preconditions.checkNotNull;

public class RandomFullClipboardPattern extends AbstractPattern {
    private final Extent extent;
    private final MutableBlockVector3 mutable = new MutableBlockVector3();
    private final List<ClipboardHolder> clipboards;
    private boolean randomRotate;
    private boolean randomFlip;

    public RandomFullClipboardPattern(Extent extent, List<ClipboardHolder> clipboards, boolean randomRotate, boolean randomFlip) {
        checkNotNull(clipboards);
        this.clipboards = clipboards;
        this.extent = extent;
        this.randomRotate = randomRotate;
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 setPosition, BlockVector3 getPosition) throws WorldEditException {
        ClipboardHolder holder = clipboards.get(PseudoRandom.random.random(clipboards.size()));
        AffineTransform transform = new AffineTransform();
        if (randomRotate) {
            transform = transform.rotateY(PseudoRandom.random.random(4) * 90);
            holder.setTransform(new AffineTransform().rotateY(PseudoRandom.random.random(4) * 90));
        }
        if (randomFlip) {
            transform = transform.scale(Vector3.at(1, 0, 0).multiply(-2).add(1, 1, 1));
        }
        if (!transform.isIdentity()) {
            holder.setTransform(transform);
        }
        Clipboard clipboard = holder.getClipboard();
        Schematic schematic = new Schematic(clipboard);
        Transform newTransform = holder.getTransform();
        if (newTransform.isIdentity()) {
            schematic.paste(extent, setPosition, false);
        } else {
            schematic.paste(extent, setPosition, false, newTransform);
        }
        return true;
    }

    @Override
    public BaseBlock apply(BlockVector3 position) {
        throw new IllegalStateException("Incorrect use. This pattern can only be applied to an extent!");
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        throw new NotSerializableException("Clipboard cannot be serialized!");
    }
}
