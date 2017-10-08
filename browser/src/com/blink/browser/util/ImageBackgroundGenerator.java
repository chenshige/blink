package com.blink.browser.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.util.ArrayMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 根据图片生成背景色，使用函数getBackgroundColor即可
 *
 * 根据图片像素距离找出在限定范围以内的像素分组，从数量最多的组当中选取最具有代表性的颜色作为背景色
 */
public class ImageBackgroundGenerator {
    private static final int MAX_COLOUR_DISTANCE = 49;//近似颜色的距离范围的平方，经验值

    /**
     * Generate a background colour from the bitmap
     *
     * @param bitmap bitmap which the background colour should be generated
     * @return the background colour, in Colour
     */
    public static int getBackgroundColor(Bitmap bitmap) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        //像素组，key是代表色，value是距离在MAX_COLOUR_DISTANCE以内的颜色集合
        Map<Integer, List<Integer>> pixelsGroup = new ArrayMap<>();
        int length = height / 5;//在图片的角采样范围
        int step = length / 30;//采样最多30个点
        if (step < 1)
            step = 1;
        //在三个角分别取样，图标有的会有一个角上带有标示
        for(int h = 1; h < length; h += step ){
            int x = h;
            if (x >= width) {
                x = width - 1;
            }
            if (x > step * 10) {//图标一般有圆角，忽略圆角的像素
                //左上角
                addPixelToPixelsGroup(bitmap.getPixel(x, h), pixelsGroup);
                //右上角
                addPixelToPixelsGroup(bitmap.getPixel(width - x, h), pixelsGroup);
                //右下角
                addPixelToPixelsGroup(bitmap.getPixel(width - x, height - h), pixelsGroup);
            }
            x = length - h;
            if (x == h) continue;//Duplicated
            if (x >= width) {
                x = width - 1;
            }
            //左上角
            addPixelToPixelsGroup(bitmap.getPixel(x, h), pixelsGroup);
            //右上角
            addPixelToPixelsGroup(bitmap.getPixel(width - x, h), pixelsGroup);
            //右下角
            addPixelToPixelsGroup(bitmap.getPixel(width - x, height - h), pixelsGroup);
        }

        List<Integer> ret = new ArrayList<>();
        int max = 0;
        for(Integer centerColour : pixelsGroup.keySet()){
            List<Integer> group = pixelsGroup.get(centerColour);
            if (group.size() > max){
                ret = group;
                max = group.size();
            }
        }

        return getMostColour(ret);
    }

    /**
     * 不同的像素分辨计算出现的次数, Hashmap的Key存储颜色像素，Value存储出现次数
     */
    private static class HashBag<K> extends ArrayMap<K, Integer> {

        public HashBag() {
            super();
        }

        public int getCount(K value) {
            if (get(value) == null) {
                return 0;
            } else {
                return get(value);
            }
        }

        public void add(K value) {
            if (get(value) == null) {
                put(value, 1);
            } else {
                put(value, get(value) + 1);
            }
        }

        public Iterator<K> iterator() {
            return keySet().iterator();
        }
    }

    /**
     * 从一个颜色集合里面找出来这些颜色里面最具备代表性的颜色
     * 查找这些颜色里面数量最多的一个颜色，作为最具有代表性的颜色
     *
     * @param colours   距离在MAX_COLOUR_DISTANCE以内的颜色集合以内的颜色集合
     * @return  最有代表性的颜色
     */
    private static int getMostColour(List<Integer> colours){
        HashBag<Integer> bag = new HashBag<>();
        for(Integer c : colours){
            bag.add(c);
        }

        int max = 0;
        int ret = Color.TRANSPARENT;
        Iterator<Integer> iterator = bag.iterator();
        while (iterator.hasNext()) {
            Integer color = iterator.next();
            int colorCount = bag.getCount(color);
            if (colorCount > max){
                max = colorCount;
                ret = color;
            }
        }
        return ret;
    }

    /**
     * 计算两个颜色之间的距离 https://en.wikipedia.org/wiki/Color_difference
     * @param colour1   ARGB形式的颜色，Alpha被忽略
     * @param colour2
     * @return  颜色的距离
     */
    private static double distanceOfColour(int colour1, int colour2){
        int r1 = Color.red(colour1);
        int g1 = Color.green(colour1);
        int b1 = Color.blue(colour1);
        int r2 = Color.red(colour2);
        int g2 = Color.green(colour2);
        int b2 = Color.blue(colour2);
        int r = r1 - r2;
        int g = g1 - g2;
        int b = b1 - b2;

        //使用平方计算，忽略开平方根
        return r * r + g * g + b * b;
    }

    /**
     * 把像素添加到像素组，按照距离为MAX_COLOUR_DISTANCE以内分组
     * @param colour
     * @param pixelsGroup
     */
    private static void addPixelToPixelsGroup(int colour, Map<Integer, List<Integer>> pixelsGroup){
        for(Integer centerColour : pixelsGroup.keySet()){
            double distance = distanceOfColour(colour, centerColour);
            if (distance < MAX_COLOUR_DISTANCE) {
                List<Integer> group = pixelsGroup.get(centerColour);
                group.add(colour);
                //TODO 应该在这里重新更新key更加准确，但是计算消耗太大达到30倍，暂时关闭
                /*
                pixelsGroup.remove(centerColour);
                centerColour = getMostColour(group);
                pixelsGroup.put(centerColour, group);
                */

                return;
            }
        }
        //没有找到对应组别，就新建一个
        List<Integer> group = new ArrayList<>();
        group.add(colour);
        pixelsGroup.put(colour, group);
    }
}
