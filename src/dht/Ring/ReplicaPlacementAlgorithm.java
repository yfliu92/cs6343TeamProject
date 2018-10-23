package dht.Ring;

import java.util.List;

public interface ReplicaPlacementAlgorithm {

    List<PhysicalNode> getReplicas(LookupTable table, VirtualNode node);

    List<PhysicalNode> getReplicas(LookupTable table, int hash);

}
