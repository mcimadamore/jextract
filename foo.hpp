 class Point2d {
     int x;
     int y;

     public:
     Point2d(int x, int y);
     virtual int distance();
  };

  class Point3d : Point2d {
     int z;

     public:
     Point3d(int x, int y, int z);
     virtual int distance();
  };