package water.api;

import water.H2O;
import water.H2ONode;
import water.Paxos;

class CloudHandler extends Handler {
  @SuppressWarnings("unused") // called through reflection by RequestServer
  public CloudV3 status(int version, CloudV3 cloud) {
    // TODO: this really ought to be in the water package
    cloud.version = H2O.ABV.projectVersion();

    cloud.node_idx = H2O.SELF.index();
    if (cloud.node_idx < 0) {
      // This is very special.  This can happen in sparkling water (i.e. client mode).
      // The client has a negative array index.  In that case, force to 0.
      // Real worker nodes have indexes 0 or greater.
      cloud.node_idx = 0;
    }

    cloud.cloud_name = H2O.ARGS.name;
    cloud.cloud_size = H2O.CLOUD.size();
    cloud.cloud_uptime_millis = System.currentTimeMillis() - H2O.START_TIME_MILLIS.get();
    cloud.consensus = Paxos._commonKnowledge;
    cloud.locked = Paxos._cloudLocked;

    // Fetch and calculate cloud metrics from individual node metrics.
    H2ONode[] members = H2O.CLOUD.members();
    cloud.bad_nodes = 0;
    cloud.cloud_healthy = true;
    if (null != members) {
      cloud.nodes = new CloudV3.NodeV3[members.length];
      for (int i = 0; i < members.length; i++) {
        cloud.nodes[i] = new CloudV3.NodeV3(members[i], cloud.skip_ticks);
        if (! cloud.nodes[i].healthy) {
          cloud.cloud_healthy = false;
          cloud.bad_nodes++;
        }
      }
    }

    return cloud;
  }
}

