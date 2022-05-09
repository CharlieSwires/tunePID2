package tunePID2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * This class contains two things:
 * 1. A thread for animating a possible intercept using chosen constants
 * 2. A genetic algorithm which is used with 2000 guesses at values followed
 * by a trial on 100 random cases to see how good each guess is. The top two
 * by distance squared least are bread from to make another 2000 with +-5%
 * mutation.
 * 
 * @author charl
 *
 */
public class TunePID2 extends JPanel{

    /**
     * Globals
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
    float initialTheta = 0.0f;
    static final double ACCEL = 1.0*9.81;
    Explosion e = null;
    static final int pauseMilli = 10;

    /**
     * 2d point double accuracy
     * @author charl
     *
     */
    public class TwoDPoint {
        public double x;
        public double y;
        TwoDPoint(double x, double y){
            this.x = x;
            this.y = y;
        }
        public TwoDPoint() {
        }
    }
    /**
     * Combination of the animation code for the target and missile
     * which shares code with the genetic algorithm not good SOLID oops!
     * @author charl
     *
     */
    public class PIDThread extends Thread{
        float delta_time = (float)(pauseMilli/1000.0);
        float kp=-26.714428f, ki=73.656044f, kd=103.96094f;
        float Kp = kp;
        float Ki = ki;
        float Kd = kd;
        float accumulation_of_error = 0.0f;
        float derivative_of_error = 0.0f;

        /**
         * Standard PID algorithm
         * @param error
         * @return
         */
        public float pid(float error) {
            //PID
            accumulation_of_error += error * delta_time;
            accumulation_of_error = (float) ((accumulation_of_error < Math.PI/8.0)?(accumulation_of_error > -Math.PI/8.0)?accumulation_of_error:-Math.PI/8.0:Math.PI/8.0);                    
            derivative_of_error = (error - last_error) / delta_time;
            last_error = error;
            output = (error * Kp) + (accumulation_of_error * Ki) + (derivative_of_error * Kd);
            return output;
        }

        /**
         * runner for animation
         */
        @Override
        public void run() {
            accumulation_of_error = 0.0f;
            derivative_of_error = 0.0f;
            while(true) {
                while(!start) {
                    try {
                        Thread.sleep(pauseMilli);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                init();
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
                            ) {
                        //                    && ((you.get(index).y-enemy.get(index).y)*
                        //                            (you.get(index).y-enemy.get(index).y)+
                        //                            (you.get(index).x-enemy.get(index).x)*
                        //                            (you.get(index).x-enemy.get(index).x)) < 10000.0) {

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
                        initialTheta = (float) Math.atan2(intlDirection.y,intlDirection.x);
                        enemyv.x = ACCEL*intlDirection.x*delta_time/amplitude;
                        enemyv.y = ACCEL*intlDirection.y*delta_time/amplitude;
                    }
                    error = (float) (Math.atan2(you.get(index).y-enemy.get(index).y,you.get(index).x-enemy.get(index).x));
                    error -= (float) (initialTheta);
                    output = pid(error);
                    //Calculate new positions
                    TwoDPoint velocity = new TwoDPoint();
                    velocity.x = enemyv.x+ACCEL*(Math.cos(output+initialTheta)-Math.sin(output+initialTheta))*delta_time;
                    velocity.y = enemyv.y+ACCEL*(Math.sin(output+initialTheta)+ Math.cos(output+initialTheta))*delta_time;
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
                    if(index >= 10000) {
                        start = false;
                        break;
                    }
                    try {
                        Thread.sleep(pauseMilli);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                e = new Explosion((int)enemy.get(index).x+width/2, height/2 -(int)enemy.get(index).y);
                for(int i=0; i < 255;i++) {
                    try {
                        pt.repaint();
                        Thread.sleep(pauseMilli);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                e = null;
            }
        }

        /**
         * init for runner
         */
        public void init() {
            enemy = new ArrayList<TwoDPoint>();
            ep.x = -500.0;
            ep.y = 0.0;
            enemy.add(ep);
            you = new ArrayList<TwoDPoint>();
            mp.x = 500.0;
            mp.y = 400.0;
            you.add(mp);
            enemyv = new TwoDPoint();
            enemyv.x = 5.0;
            enemyv.y = 0.0;
            youv = new TwoDPoint();
            youv.x = 0.0;
            youv.y = -100.0;

        }
        /**
         * init for copyOfRun
         * @param rand
         */
        public void init2(Random rand) {
            enemy = new ArrayList<TwoDPoint>();
            ep.x = 0.0;
            ep.y = 0.0;
            enemy.add(ep);
            you = new ArrayList<TwoDPoint>();
            mp.x = 2000.0*(rand.nextDouble()-0.5);
            mp.y = 2000.0*(rand.nextDouble()-0.5);
            you.add(mp);
            enemyv = new TwoDPoint();
            enemyv.x = 0.0;
            enemyv.y = 0.0;
            youv = new TwoDPoint();
            youv.x = 200.0*(rand.nextDouble()-0.5);
            youv.y = 200.0*(rand.nextDouble()-0.5);;

        }

        /**
         * Chromasome structure
         * @author charl
         *
         */
        public class Chromasome{
            public Chromasome(float kp, float ki, float kd, Float distanceSquared) {
                this.kp = kp;
                this.ki = ki;
                this.kd = kd;
                this.distanceSquared = distanceSquared;
            }
            public Chromasome() {
                // TODO Auto-generated constructor stub
            }
            public float kp;
            public float ki;
            public float kd;
            public Float distanceSquared;
            @Override
            public String toString() {
                return "Chromasome [kp=" + kp + ", ki=" + ki + ", kd=" + kd + ", distanceSquared="
                        + distanceSquared + "]";
            }
        }
        /**
         * Genetic algorithm
         * 1. Set 2000 guesses
         * 2. run in 100 scenarios
         * 3. sort into nearest first farthest last
         * 4. breed best 2 for 2000 and add +-5% mutation
         * 5. run in same 100 scenarios goto 3
         */
        public void geneticAlgorithm() {
            Chromasome guesses[] = new Chromasome[2000];
            for (int i = 0; i < guesses.length; i++) {
                guesses[i] = new Chromasome((float)(Math.random()*200.0-100.0),
                        (float)(Math.random()*200.0-100.0),
                        (float)(Math.random()*200.0-100.0),
                        null);
            }
            for (int i = 0; i < guesses.length; i++) {
                Random rand = new Random(0);
                guesses[i].distanceSquared = 0.0f;
                for (int noSessions = 0; noSessions < 100; noSessions++) {
                    guesses[i].distanceSquared += copyOfRun(
                            rand,
                            guesses[i].kp,
                            guesses[i].ki,
                            guesses[i].kd);

                }
            }
            List<Chromasome> guessesAsList = Arrays.asList(guesses);

            while (true) {

                guessesAsList.sort(new Comparator<Chromasome>() {

                    @Override
                    public int compare(Chromasome o1, Chromasome o2) {
                        if (o1.distanceSquared == null && o2.distanceSquared == null) {
                            return 0;
                        } else if (o1.distanceSquared == null) {
                            return 1;
                        } else if (o2.distanceSquared == null) {
                            return -1;
                        } else if (o1.distanceSquared > o2.distanceSquared){
                            return 1;
                        } else return -1;
                    }

                });
                for (int i = 0; i < 1; i++) {
                    System.out.println("i="+i+" guesses[i].distanceSquared="+guessesAsList.get(i).toString());
                }
                if (guessesAsList.get(0).distanceSquared < 5.0f) break;
                //Breed
                Chromasome top1 = guessesAsList.get(0);
                Chromasome top2 = guessesAsList.get(1);
                List<Chromasome> guessesAsList2 = new ArrayList<>();
                for (int i = 0; i < guessesAsList.size();i++) {
                    Chromasome newBorne = new Chromasome();
                    if (Math.random() > 0.5) {
                        newBorne.kp = top2.kp;
                    } else {
                        newBorne.kp = top1.kp;
                    }
                    if (Math.random() > 0.5) {
                        newBorne.ki = top2.ki;
                    } else {
                        newBorne.ki = top1.ki;
                    }
                    if (Math.random() > 0.5) {
                        newBorne.kd = top2.kd;
                    } else {
                        newBorne.kd = top1.kd;
                    }

                    //Mutation up to +-random * 5%
                    newBorne.kp *= 1.15 -Math.random()*10.0/100.0;
                    newBorne.ki *= 1.15 -Math.random()*10.0/100.0;
                    newBorne.kd *= 1.15 -Math.random()*10.0/100.0;
                    guessesAsList2.add(newBorne);

                }
                guessesAsList = guessesAsList2;
                for (int i = 0; i < guessesAsList.size(); i++) {
                    Random rand = new Random(0);
                    guessesAsList.get(i).distanceSquared = 0.0f;
                    for (int noSessions = 0; noSessions < 100; noSessions++) {
                        guessesAsList.get(i).distanceSquared += copyOfRun(
                                rand,
                                guessesAsList.get(i).kp,
                                guessesAsList.get(i).ki,
                                guessesAsList.get(i).kd);

                    }
                }
            }
        }
        /**
         * Copy of Run with parameters for Genetic Algorithm
         * @param rand
         * @param kp
         * @param ki
         * @param kd
         * @return
         */
        public Float copyOfRun(Random rand, float kp, float ki, float kd) {
            accumulation_of_error = 0.0f;
            derivative_of_error = 0.0f;
            Kp = kp;
            Ki = ki;
            Kp = kp;
            init2(rand);
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
            while(index <= 10000) {
                //calculate error
                if (index > 1 && ((you.get(index).y-enemy.get(index).y)*
                        (you.get(index).y-enemy.get(index).y)+
                        (you.get(index).x-enemy.get(index).x)*
                        (you.get(index).x-enemy.get(index).x)) >
                ((you.get(index-1).y-enemy.get(index-1).y)*
                        (you.get(index-1).y-enemy.get(index-1).y)+
                        (you.get(index-1).x-enemy.get(index-1).x)*
                        (you.get(index-1).x-enemy.get(index-1).x))
                        ) {

                    /**
                     * return closest point of approach
                     */
                    return (float) ((you.get(index).y-enemy.get(index).y)*
                            (you.get(index).y-enemy.get(index).y)+
                            (you.get(index).x-enemy.get(index).x)*
                            (you.get(index).x-enemy.get(index).x));
                }
                //Initial vector
                if (index == 0) {
                    intlDirection.x = you.get(index).x-enemy.get(index).x;
                    intlDirection.y = you.get(index).y-enemy.get(index).y;
                    amplitude = Math.sqrt(intlDirection.x*intlDirection.x+
                            intlDirection.y*intlDirection.y);
                    initialTheta = (float) Math.atan2(intlDirection.y,intlDirection.x);
                    enemyv.x = ACCEL*intlDirection.x*delta_time/amplitude;
                    enemyv.y = ACCEL*intlDirection.y*delta_time/amplitude;
                }
                error = (float) (Math.atan2(you.get(index).y-enemy.get(index).y,you.get(index).x-enemy.get(index).x));
                error -= (float) (initialTheta);
                output = pid(error);
                //Calculate new positions
                //limit output +-45degrees
                //output = (float) ((output < Math.PI/8.0)?(output > -Math.PI/8.0)?output:-Math.PI/8.0:Math.PI/8.0);
                TwoDPoint velocity = new TwoDPoint();
                velocity.x = enemyv.x+ACCEL*(Math.cos(output+initialTheta)-Math.sin(output+initialTheta))*delta_time;
                velocity.y = enemyv.y+ACCEL*(Math.sin(output+initialTheta)+ Math.cos(output+initialTheta))*delta_time;
                positionx += velocity.x;
                positiony += velocity.y;
                enemyv.x = velocity.x;
                enemyv.y = velocity.y;

                enemy.add(new TwoDPoint(positionx,positiony));
                position2x += youv.x*delta_time;
                position2y += youv.y*delta_time;

                you.add(new TwoDPoint(position2x,position2y));
                index++;

            }
            /**
             * return null if doesn't finish in 10,000 iterations
             */
            return null;
        }
    }
    /**
     * Called by repaint for the animation runner.
     */
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
    /**
     * Explosion type for drawing the collision between missile and target.
     * @author charl
     *
     */
    public class Explosion {
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

        public void draw(Graphics g) {
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
    /**
     * Set up the runner screen and buttons
     * @author charl
     *
     */
    public class PaintDemo{

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

    /**
     * For kicking everything off
     * @param args
     */
    public static void main(String[] args) {
        pt = new TunePID2();
        pt.thread = pt.new PIDThread();
        if(args.length > 0 && args[0].equals("genetic")) {
            pt.thread.geneticAlgorithm();
        } else if(args.length > 0 && args[0].equals("runner")){
            pt.new PaintDemo();
            pt.thread.start();
        } else {
            System.out.println("java TunePID2 <genetic||runner>");
        }


    }

}
