package tunePID2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class TunePID2 extends JPanel{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    float error = 0.0f;
    float last_error = 0.0f;
    float output = 0.0f;
    int width = 1900;
    int height = 1000;
    JButton go = new JButton("go");
    JFrame jfrm = new JFrame("PID tune");
    boolean start = false;
    static TunePID2 pt;
    List<TwoDPoint> enemy;
    List<TwoDPoint> you;
    TwoDPoint enemyv;
    TwoDPoint youv;
    PIDThread thread;
    TwoDPoint ep = new TwoDPoint();
    TwoDPoint mp = new TwoDPoint();
    static final double ACCEL = 9.81*0.1;
    Explosion e = null;
    
    class TwoDPoint {
        public double x;
        public double y;
        TwoDPoint(double x, double y){
            this.x = x;
            this.y = y;
        }
        public TwoDPoint() {
        }
    }
    class PIDThread extends Thread{
        float delta_time = (float)(1.0/100.0);
        float Kp =100.0f;
        float Ki = 50.0f;
        float Kd = 0.0f;
        @Override
        public void run() {
            while(true) {
                while(!start) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                init();
                float accumulation_of_error = 0.0f;
                float derivative_of_error = 0.0f;
                TwoDPoint intlDirection = new TwoDPoint();
                double positionx;
                double positiony;
                positionx = ep.x;
                positiony = ep.y;
                double position2x;
                double position2y;
                position2x = mp.x;
                position2y = mp.y;

                double amplitude = 0.0;
                int index = 0;
                while(start) {
                    //calculate error
                    if (index > 1 && ((you.get(index).y-enemy.get(index).y)*
                            (you.get(index).y-enemy.get(index).y)+
                            (you.get(index).x-enemy.get(index).x)*
                            (you.get(index).x-enemy.get(index).x)) >
                    ((you.get(index-1).y-enemy.get(index-1).y)*
                            (you.get(index-1).y-enemy.get(index-1).y)+
                            (you.get(index-1).x-enemy.get(index-1).x)*
                            (you.get(index-1).x-enemy.get(index-1).x))
                    && ((you.get(index).y-enemy.get(index).y)*
                            (you.get(index).y-enemy.get(index).y)+
                            (you.get(index).x-enemy.get(index).x)*
                            (you.get(index).x-enemy.get(index).x)) < 10000.0) {

                        System.out.println("Bang");
                        start = false;
                        break;
                    }
                    //Initial vector
                    if (index == 0) {
                        intlDirection.x = you.get(index).x-enemy.get(index).x;
                        intlDirection.y = you.get(index).y-enemy.get(index).y;
                        amplitude = Math.sqrt(intlDirection.x*intlDirection.x+
                                intlDirection.y*intlDirection.y);
                  
                        enemyv.x = ACCEL*intlDirection.x*delta_time/amplitude;
                        enemyv.y = ACCEL*intlDirection.y*delta_time/amplitude;
                    }
                    System.out.println("you.get(index).y-enemy.get(index).y-intl="+(you.get(index).y-enemy.get(index).y)+","+(you.get(index).x-enemy.get(index).x));
                    error = (float) (Math.atan2(you.get(index).y-enemy.get(index).y,you.get(index).x-enemy.get(index).x));
                    error -= (float) (Math.atan2(enemyv.y,enemyv.x));
                    error = (float) ((error < Math.PI/8.0)?(error > -Math.PI/8.0)?error:-Math.PI/8.0:Math.PI/8.0);
                    System.out.println("error="+error+", "+(180.0*error/Math.PI));
                    //PID
                    accumulation_of_error += error * delta_time;
                    accumulation_of_error = (float) ((accumulation_of_error < Math.PI/8.0)?(accumulation_of_error > -Math.PI/8.0)?accumulation_of_error:-Math.PI/8.0:Math.PI/8.0);                    
                    derivative_of_error = (error - last_error) / delta_time;
                    last_error = error;
                    output = (error * Kp) + (accumulation_of_error * Ki) + (derivative_of_error * Kd);
                    //Calculate new positions
                    System.out.println("output="+output);
                    //limit output +-45degrees
                    output = (float) ((output < Math.PI/8.0)?(output > -Math.PI/8.0)?output:-Math.PI/8.0:Math.PI/8.0);
                    System.out.println("output="+output);
                    TwoDPoint velocity = new TwoDPoint();
                    System.out.println("enemyv.x="+enemyv.x+", y="+enemyv.y);
                    intlDirection.x = you.get(index).x-enemy.get(index).x;
                    intlDirection.y = you.get(index).y-enemy.get(index).y;
                    amplitude = Math.sqrt(intlDirection.x*intlDirection.x+
                            intlDirection.y*intlDirection.y);
                    velocity.x = enemyv.x+ACCEL*(Math.cos(output)*enemyv.x-Math.sin(output)*enemyv.y)*delta_time;
                    velocity.y = enemyv.y+ACCEL*(Math.sin(output)*enemyv.x + Math.cos(output)*enemyv.y)*delta_time;
                    positionx += velocity.x;
                    positiony += velocity.y;
                    enemyv.x = velocity.x;
                    enemyv.y = velocity.y;
                    
                    enemy.add(new TwoDPoint(positionx,positiony));
                    position2x += youv.x*delta_time;
                    position2y += youv.y*delta_time;
                    
                    you.add(new TwoDPoint(position2x,position2y));
                    index++;
                    pt.repaint();
                    if(index >= 1000) {
                        start = false;
                        break;
                    }
                    System.out.println("Index="+index);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                e = new Explosion((int)enemy.get(index).x+width/2, height/2 -(int)enemy.get(index).y);
                for(int i=0; i < 255;i++) {
                    try {
                        pt.repaint();
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
 
                }
                e = null;
            }
        }
        private void init() {
            enemy = new ArrayList<TwoDPoint>();
            ep.x = -500.0;
            ep.y = 0.0;
            enemy.add(ep);
            you = new ArrayList<TwoDPoint>();
            mp.x = 500.0;
            mp.y = 400.0;
            you.add(mp);
            enemyv = new TwoDPoint();
            enemyv.x = 1.0;
            enemyv.y = 0.0;
            youv = new TwoDPoint();
            youv.x = 0.0;
            youv.y = -60.0;

        }
    }
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int height = getHeight();
        int width = getWidth();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        if (enemy != null && you != null) {
            for(int i = 1; i < enemy.size() && i < you.size();i++) {
                g.setColor(Color.RED);
                g.drawLine((int)enemy.get(i-1).x+width/2, height/2 -(int)enemy.get(i-1).y, width/2+(int)enemy.get(i).x, height/2-(int)enemy.get(i).y);
                g.setColor(Color.GREEN);
                g.drawLine((int)you.get(i-1).x+width/2, height/2 -(int)you.get(i-1).y, width/2+(int)you.get(i).x, height/2-(int)you.get(i).y);
            }
            if (e != null) e.draw(g);

        }

    }
    class Explosion {
        private static final int NO_POINTS = 200;
        private double x[] = null;
        private double y[] = null;
        private double dx[] = null;
        private double dy[] = null;
        private int count = 255;
        public Explosion(int x, int y) {
            this.x = new double[NO_POINTS];
            this.y = new double[NO_POINTS];
            this.dx = new double[NO_POINTS];
            this.dy = new double[NO_POINTS];
            for (int i = 0; i < NO_POINTS;i++) {
                this.x[i]=x;
                this.y[i] =y;
                double bearing = 2.0 * Math.PI * Math.random();
                double speed = Math.random()*5.0;
                this.dx[i] = speed*Math.sin(bearing);;
                this.dy[i] = speed*Math.cos(bearing);
            }
        }

        void draw(Graphics g) {
            Color c = new Color(count,count,count--);
            g.setColor(c);
            for (int i = 0; i < NO_POINTS;i++) {
                g.fillRect((int)x[i], (int)y[i], 5, 5);
                this.x[i]+=dx[i];
                this.y[i]+=dy[i];
            }
        }
        public int getCount() {
            return count;
        }
    }


    class PaintDemo{

        PaintDemo(){
            jfrm.setSize(width, height);
            jfrm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


            jfrm.setLayout(new BorderLayout());

            JPanel temp = new JPanel();
            go.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {

                    start = !start;   
                    System.out.println("start="+start);
                }

            });
            temp.add(go);

            jfrm.add(temp, BorderLayout.NORTH);
            jfrm.add(pt, BorderLayout.CENTER);

            jfrm.setVisible(true);
        }
    }

    public static void main(String[] args) {
        pt = new TunePID2();
        pt.thread = pt.new PIDThread();
        pt.new PaintDemo();
        pt.thread.start();


    }

}
