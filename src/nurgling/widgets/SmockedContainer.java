package nurgling.widgets;

import haven.*;
import haven.res.lib.itemtex.ItemTex;
import nurgling.NConfig;
import nurgling.NGItem;
import nurgling.NStyle;
import nurgling.NUtils;
import nurgling.areas.NArea;
import nurgling.tools.NDTarget;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class SmockedContainer extends Widget implements NDTarget
{
    static Color bg = new Color(30,40,40,160);
    IngredientContainer.Ingredient ing;
    IconItem iconItem = null;
    public SmockedContainer()
    {
        this.type = type;
        this.sz = UI.scale(new Coord(50,50));
    }

    String type;

    @Override
    public void draw(GOut g)
    {
        g.chcolor(bg);
        g.frect(Coord.z, g.sz());
        super.draw(g);
    }

    public void addIcon(JSONObject res)
    {
        if(res!=null && res.get("name")!=null)
        {
            ing = new IngredientContainer.Ingredient((String)res.get("name"), ItemTex.create(res));
            if(iconItem == null)
            {
                add(iconItem = new IconItem(ing.name, ing.image),UI.scale(9,9));
            }
            else
            {
                iconItem.update(ing.name, ing.image);
            }
            iconItem.src = res;
        }
    }


    @Override
    public boolean drop(WItem item, Coord cc, Coord ul)
    {

        String name = ((NGItem) item.item).name();
        JSONObject res = ItemTex.save(item.item.spr);
        addItem(name, res);

        return true;
    }

    public void addItem(String name, JSONObject res)
    {
        if (res != null)
        {
            res.put("name", name);
            res.put("type", NArea.Ingredient.Type.CONTAINER.toString());
            addIcon(res);
        }
    }

}