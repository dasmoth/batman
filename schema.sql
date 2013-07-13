drop table if exists medip_roi;
create table medip_roi (
     id             int primary key auto_increment not null,
     roi_name       varchar(40) not null,

     unique key (roi_name)
);

drop table if exists medip_probe;
create table medip_probe (
     id             int primary key auto_increment not null,
     roi            int not null,
     probe_name     varchar(40) not null,
     chr            varchar(40) not null,
     min_pos        int not null,
     max_pos        int not null,

     unique key (probe_name),
     key roi_idx (roi),
     key probe_genomic_idx (chr, min_pos, max_pos)
);

drop table if exists medip_expt;
create table medip_expt (
     id             int primary key auto_increment not null,
     expt_name      varchar(40) not null,
     array_id       varchar(40) not null,
     tissue         varchar(40) not null,
     sample         varchar(40) not null,
     cy3            enum('IP', 'INPUT', 'UNKNOWN'),
     cy5            enum('IP', 'INPUT', 'UNKNOWN')
);

drop table if exists medip_data;
create table medip_data (
     probe          int not null,
     expt           int not null,
     log_ratio      double,

     unique key data_idx (probe, expt)
);

drop table if exists xmeth_coupling_profile;
create table xmeth_coupling_profile (
    profile_name           varchar(40) not null,
    offset                 int not null,
    coupling               double not null,

    unique key pos_idx (profile_name, offset)
);

drop table if exists xmeth_array_meta;
create table xmeth_array_meta (
    expt                   int not null,
    response               double not null,
    baseline               double not null,

    unique key expt_idx (expt)
);

drop table if exists xmeth_genome_sequence;
create table xmeth_genome_sequence (
     assembly              varchar(40) not null,
     seq_name              varchar(40) not null,
     dna                   longtext not null,

     unique key seq_idx (assembly, seq_name)
);

drop table if exists xmeth_genome_fragment;
create table xmeth_genome_fragment (
     assembly              varchar(40) not null,
     seq_name              varchar(40) not null,
     seq_min               int not null,
     seq_max		   int not null,
     dna                   longtext,

     unique key seq_frag_idx (seq_name, seq_min, seq_max)
);

drop table if exists xmeth_array_grouping;
create table xmeth_array_grouping (
    expt                  int not null,
    expt_group            varchar(40) not null,
    
    key group_idx (expt_group)
);

drop table if exists xmeth_window_quantitation;
create table xmeth_window_quantitation (
    seq_name             varchar(40) not null,
    seq_min		 int not null,
    seq_max		 int not null,
    tissue		 varchar(40) not null,
    meth_mean		 double not null,
    meth_var		 double not null,

    key wq_seq_idx (seq_name, seq_min, seq_max)
);

drop table if exists xmeth_window_beta;
create table xmeth_window_beta (
    seq_name             varchar(40) not null,
    seq_min		 int not null,
    seq_max		 int not null,
    tissue		 varchar(40) not null,
    meth_alpha		 double not null,
    meth_beta		 double not null,

    key wb_seq_idx (seq_name, seq_min, seq_max)
);