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

    class TwoDPoint {
        public double x;
        public double y;
    }
    class PIDThread extends Thread{
        float delta_time = (float)(1.0/60.0);
        float Kp = 10.0f;
        float Ki = 1.0f;
        float Kd = 1.0f;
        @Override
        public void run() {
            while(true) {
                while(!start) {
                    try {
                        pt.thread.sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                init();
                float accumulation_of_error = 0.0f;
                float derivative_of_error = 0.0f;
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
                            (you.get(index).x-enemy.get(index).x)) < 100.0) {
                        System.out.println("Bang");
                        start = false;
                        break;
                    }
                    error = (float) Math.atan2(you.get(index).y-enemy.get(index).y,you.get(index).x-enemy.get(index).x);
                    //PID
                    accumulation_of_error += error * delta_time;
                    derivative_of_error = (error - last_error) / delta_time;
                    last_error = error;
                    output = (error * Kp) + (accumulation_of_error * Ki) + (derivative_of_error * Kd);
                    //Calculate new positions
                    TwoDPoint velocity = new TwoDPoint();
                    velocity.x = enemyv.x + 10.0*9.81*delta_time*Math.cos(output);
                    velocity.y = enemyv.y + 10.0*9.81*delta_time*Math.sin(output);
                    TwoDPoint position = new TwoDPoint();
                    position.x += velocity.x*delta_time;
                    position.y += velocity.y*delta_time;
                    enemyv.x = velocity.x;
                    enemyv.y = velocity.y;
                    enemy.add(position);
                    TwoDPoint position2 = new TwoDPoint();
                    position2.x = you.get(index).x+youv.x*delta_time;
                    position2.y = you.get(index).y+youv.y*delta_time;
                    you.add(position2);
                    index++;
                    pt.repaint();
                }
            }
        }
        private void init() {
            enemy = new ArrayList<TwoDPoint>();
            TwoDPoint ep = new TwoDPoint();
            ep.x = 0.0;
            ep.y = 0.0;
            enemy.add(ep);
            you = new ArrayList<TwoDPoint>();
            TwoDPoint mp = new TwoDPoint();
            mp.x = 500.0;
            mp.y = 0.0;
            you.add(mp);
            enemyv = new TwoDPoint();
            enemyv.x = 0.0;
            enemyv.y = 0.0;
            youv = new TwoDPoint();
            youv.x = 0.0;
            youv.y = 1.0;

        }
    }
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int height = getHeight();
        int width = getWidth();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        if (enemy != null) {
            for(int i = 1; i < enemy.size();i++) {
                g.setColor(Color.RED);
                g.drawLine((int)enemy.get(i-1).x+width/2, height/2 -(int)enemy.get(i-1).y, width/2+(int)enemy.get(i).x, height/2-(int)enemy.get(i).y);
                g.setColor(Color.GREEN);
                g.drawLine((int)you.get(i-1).x+width/2, height/2 -(int)you.get(i-1).y, width/2+(int)you.get(i).x, height/2-(int)you.get(i).y);
            }
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
