create table User(userId Varchar(255) not null, password Varchar(255) not null, primary key(userId));

create table Money(userId Varchar(255) not null, usedMoney real not null, totalMoney real not null, primary key(userId), foreign key(userId) references User(userId));

create table Item(id bigint not null, userId Varchar(255) not null, name NVarchar(255), price real, isOut bit not null, classify tinyint not null, time datetime not null, primary key(id), foreign key(userId) references User(userId));

