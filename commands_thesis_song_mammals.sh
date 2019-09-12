var='song_mammals.40'
#test_data/amnios248nt_phymlspr_rooted.trees
#rm /home/nishat/Documents/Thesis/Astral_Stelar/STELAR_master/test_data/$var.tre

#cp /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.tre /home/nishat/Documents/Thesis/Astral_Stelar/STELAR_master/test_data/ 

#perl /home/nishat/Documents/Thesis/Astral_Stelar/STELAR_master/AfterPhylo.pl -unlabeled -topology /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.tre

#java -jar /home/nishat/Documents/Thesis/Astral_Stelar/STELAR_master/STELAR.jar -i /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.gene.tre -st /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.out.tre

#java -jar /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_master/astral.5.6.3.jar -i /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.gene.tre -q /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.tre

rm ~/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.tre 

java -jar /home/nishat/Documents/Thesis/Astral_Stelar/STELAR_master/STELAR.jar -i /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.gene.tre -o /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.tre
java -jar /home/nishat/Documents/Thesis/Astral_Stelar/STELAR_master/STELAR.jar -i /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.gene.tre -st /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.tre
java -jar /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_master/astral.5.6.3.jar -i /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.gene.tre -q /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.tre

java -jar /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_master/astral.5.6.3.jar -i /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.gene.tre -o /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.tre
perl /home/nishat/Documents/Thesis/Astral_Stelar/STELAR_master/AfterPhylo.pl -unlabeled -topology ~/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.tre
java -jar /home/nishat/Documents/Thesis/Astral_Stelar/STELAR_master/STELAR.jar -i /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.gene.tre -st /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.out.tre


#java -jar /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_master/astral.5.6.3.jar -i /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.gene.tre -q /home/nishat/Documents/Thesis/Astral_Stelar/ASTRAL_STELARmerged/test_data/$var.tre


