import org.bukkit.Material;

public class DumpMaterials {
    public static void main(String[] args) {
        for (Material material : Material.values()) {
            if (material.isAir()) {
                continue;
            }
            System.out.println(material.name());
        }
    }
}
