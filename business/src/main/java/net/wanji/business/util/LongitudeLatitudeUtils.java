package net.wanji.business.util;

import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GeodeticCurve;
import org.gavaghan.geodesy.GlobalCoordinates;

import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author hanchaoyong
 * @version 1.0
 * @className LongitudeLatitudeUtils
 * @description TODO
 * @date 2023-04-21 18:54
 **/
public class LongitudeLatitudeUtils {

  /**
   * 计算关键点路径长度
   *
   * @param points
   * @return
   */
  public static double fullLength(List<Point2D.Double> points) {
    double fullLength = 0.0;
    for (int i = 0; i < points.size() - 1; i++) {
      Point2D.Double current = points.get(i);
      Point2D.Double next = points.get(i + 1);
      fullLength += ellipsoidalDistance(current, next);
    }
    return fullLength;
  }

  /**
   * 计算是否经过门架
   * 算法：时间戳排序，顺序经过，圆外、圆内、圆外，认为已经通过门架
   *
   * @param points
   * @param portalLL 门架中心点
   * @param radius   半径
   * @return
   */
  public static boolean isCrossSection(List<Point2D.Double> points,
      Point2D.Double portalLL, BigDecimal radius) {

    int atLastThree = 3;
    if (points.size() < atLastThree) {
      return false;
    }
    boolean before = false;
    boolean in = false;
    boolean after = false;
    for (Point2D.Double point : points) {
      boolean inCycle = isInCycle(point, portalLL, radius);
      if (!before) {
        before = inCycle;
      } else if (!in) {
        in = inCycle;
      } else {
        after = !inCycle;
      }
    }

    return after;
  }

  /**
   * @param point
   * @param portalLL
   * @param radius   单位（m）
   * @return
   */
  public static boolean isInCycle(Point2D.Double point, Point2D.Double portalLL,
      BigDecimal radius) {
    GeodeticCurve geodeticCurve = new GeodeticCalculator().calculateGeodeticCurve(
        Ellipsoid.WGS84, new GlobalCoordinates(portalLL.x, portalLL.y),
        new GlobalCoordinates(point.x, point.y));
    return radius.compareTo(
        BigDecimal.valueOf(geodeticCurve.getEllipsoidalDistance())) >= 0;
  }

  public static double ellipsoidalDistance(Point2D.Double source,
      Point2D.Double target) {
    return ellipsoidalDistance(source.x, source.y, target.x, target.y);
  }

  public static double ellipsoidalDistance(double sourceX, double sourceY,
      double targetX, double targetY) {
    GeodeticCurve geodeticCurve = new GeodeticCalculator().calculateGeodeticCurve(
        Ellipsoid.WGS84, new GlobalCoordinates(sourceX, sourceY),
        new GlobalCoordinates(targetX, targetY));
    return geodeticCurve.getEllipsoidalDistance();
  }

}
