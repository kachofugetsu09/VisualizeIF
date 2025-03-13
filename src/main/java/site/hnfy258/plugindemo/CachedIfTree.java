package site.hnfy258.plugindemo;

public class CachedIfTree {
    final IFTreeNode tree;
    final long modificationStamp;

    CachedIfTree(IFTreeNode tree, long modificationStamp) {
        this.tree = tree;
        this.modificationStamp = modificationStamp;
    }
}

