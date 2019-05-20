import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class FCAN {
    private int[][] tdm;
    private List<Cluster> clusters;
    private final double MAX_DISTANCE = 11;
    private final static int NUM_WEIGHTS = 31;

    public FCAN(){
        this.tdm = DataPrep.sendTDM();
        this.clusters = new CopyOnWriteArrayList<>();
    }

    public int addPattern(int sentNum, int[] pattern){
        if (sentNum == 10 || sentNum == 12){
            int catchDebug = 0;
        }
        if (sentNum == 0){
            this.clusters.add(new Cluster(sentNum, pattern));
            return 0;
        }
        else{
            Cluster cluster = findMinDisCluster(sentNum, pattern);
            if (cluster.calcDistance(pattern) > MAX_DISTANCE){
                this.clusters.add(new Cluster(sentNum, pattern));
            }
            else {
                cluster.addMember(sentNum);
                cluster.updateWeights(pattern);
            }
        }
        return -2;
    }

    public void redoClusters(){
        for (Cluster outerClust : this.clusters){
            for (Integer member : outerClust.members){
                double min = outerClust.calcDistance(this.tdm[member]);
                for (Cluster innerClust : this.clusters){
                    double dist = innerClust.calcDistance(this.tdm[member]);
                    if (dist < min){
                        min = dist;
                        outerClust.removeMember(member);
                        innerClust.addMember(member);
                    }
                }
            }
        }
    }

    public Cluster findMinDisCluster(int sentNum, int[] pattern){
        double min = Double.MAX_VALUE;
        Cluster target = null;
        for (Cluster cluster : this.clusters){
            double distance = cluster.calcDistance(pattern);
            if (distance<min){
                min = distance;
                target = cluster;
            }
        }
        return target;
    }

    class Cluster{
        int num_weights = NUM_WEIGHTS;
        int m;
        double alpha = .9;
        double[] weights;
        List<Integer> members;

        public Cluster(int sentNum, int[] sentence){
            this.members = new CopyOnWriteArrayList<>();
            members.add(sentNum);
            this.m = 1;
            this.weights = new double[this.num_weights];
            for (int i = 0; i<this.num_weights; i++){
                this.weights[i] = sentence[i];
            }
        }

        public void addMember(int i){
            this.members.add(i);
        }

        public void removeMember(int i){
            for (int index = 0; index<this.members.size(); index++){
                if (this.members.get(index).equals(i)){
                    this.members.remove(index);
                    return;
                }
            }
        }

        public String memberToString(){
            String result = "";
            Iterator<Integer> through = this.members.iterator();
            while (through.hasNext()){
                int mem = through.next();
                mem++;
                result += (mem + " ");
            }
            return result;
        }

        public double calcDistance(int[] sentence){
            double totalDistance = 0;
            for (int i = 0; i<this.num_weights; i++){
                double distance = Math.abs(this.weights[i] - (double) sentence[i]);
                totalDistance += distance;
            }
            return totalDistance;
        }

        public void updateWeights(int[] sentence){
            for (int i = 0; i<this.num_weights; i++){
                double wk = ((this.m*this.weights[i]) + (this.alpha*sentence[i])) / (this.m+1);
                this.weights[i] = wk;
            }
            this.m++;
        }
    }

    public static void main(String[] args){
        FCAN fcan = new FCAN();
        int status = -3;
        for (int i = 0; i<fcan.tdm.length; i++){
            status = fcan.addPattern(i, fcan.tdm[i]);
            if (status<0){
                fcan.redoClusters();
            }
        }

        int count = 1;
        for (Cluster cluster : fcan.clusters){
            if (cluster.members.size() < 1){
                fcan.clusters.remove(cluster);
            }
            else {
                System.out.println("Cluster " + count + ": " + cluster.memberToString());
                count++;
            }
        }
    }
}
