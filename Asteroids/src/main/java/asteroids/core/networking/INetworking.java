package asteroids.core.networking;

import asteroids.core.containers.Entity;
import asteroids.core.containers.ModifiableList;
import asteroids.core.containers.Transform;
import asteroids.core.graphics.Mesh;
import asteroids.core.graphics.Renderer;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;

public abstract class INetworking {
    final int portTCP = 55555;
    final int portUDP = 55556;

    protected Server server = new Server() {
        protected Connection newConnection() {
            return new NetConnection();
        }
    };
    protected Client client = new Client();

    protected boolean isServer = false;
    protected ModifiableList<INetworked> networkeds = new ModifiableList<>();
    protected HashMap<Integer, List<Object>> waitingForSerialization = new HashMap<>();

    protected ModifiableList<INetworked> queuedForRemoval = new ModifiableList<>();

    protected int networkedComponentCounter = 0;

    protected String username = "";

    private Renderer renderer;

    protected float lastDelta = 0.0f;

    public INetworking() {
        registerClass(INetworked.class);
        registerClass(NetPacket.class);
        registerClass(Transform.class);
        registerClass(Vector2f.class);
        registerClass(Vector3f.class);
        registerClass(Mesh.class);
    }

    public void addNetworkedComponent(INetworked component) {
        if (isServer) {
            component.setNetId(getNewNetId());
        }

        networkeds.add(component);
    }

    // server cant remove components before sending the message to clients
    public void queueForRemoval(INetworked component) {
        queuedForRemoval.add(component);
    }

    protected void removeQueuedComponents() {
        for (INetworked n : queuedForRemoval) {
            if (n == null) {
                continue;
            }

            n.getEntity().getRenderer().removeEntity(n.getEntity());
        }
        queuedForRemoval.clear();
    }

    public void removeNetworkedComponent(INetworked component) {
        networkeds.remove(component);
    }

    public void setIsServer(boolean isServer) {
        this.isServer = isServer;
    }

    public boolean getIsServer() {
        return isServer;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public abstract boolean isOffline();

    public abstract void init();

    public abstract void preUpdate(float deltaTime);
    public abstract void postUpdate(float deltaTime);

    public abstract void destroy();

    public abstract int getNewNetId();

    protected String encodeEntity(Entity e) {
        StringBuilder builder = new StringBuilder();
        builder.append(e.getEntityId());

        List<INetworked> list = e.getComponentsOfType(INetworked.class);
        for (INetworked n : list) {
            builder.append(";").append(n.getClass().getName()).append(";").append(n.getNetId()).append(";").append(n.getOwner());
        }

        return builder.toString();
    }

    protected Entity decodeEntity(String data) {
        String[] split = data.split(";");
        Entity e = new Entity();
        e.setEntityId(Integer.parseInt(split[0]));

        for (int i = 1; i < split.length; i += 3) {
            try {
                INetworked n = (INetworked) Class.forName(split[i]).newInstance();
                n.setNetId(Integer.parseInt(split[i + 1]));

                if (i + 2 >= split.length) {
                    n.setOwner("");
                } else {
                    n.setOwner(split[i + 2]);
                }

                e.addComponent(n);
            } catch (ClassNotFoundException ex) {
                System.out.println("Class not found.\n" + ex.getMessage());
            } catch (InstantiationException ex) {
                System.out.println("Couldn't instantiate class.\n" + ex.getMessage());
            } catch (IllegalAccessException ex) {
                System.out.println("Couldn't access class.\n" + ex.getMessage());
            }
        }

        return e;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // game has to do this
    public void registerClass(Class type) {
        server.getKryo().register(type);
        client.getKryo().register(type);
    }
}
