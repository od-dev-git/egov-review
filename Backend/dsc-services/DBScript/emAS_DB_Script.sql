
Insert into
 public.emas_admin_group 
(emas_admin_group_id,admin_group_code,admin_group_desc,admin_group_name) 
values(1,'20','Reports','Reports');

Insert
INTO public.emas_admin_group 
(emas_admin_group_id,admin_group_code,admin_group_desc,admin_group_name) 
values(2,'30','Authorise','Authorise');

-------------------------------------------------------------------------------------------------------------------------
INSERT INTO  public.super_admin_user
(superAdmin_users_id,superAdmin_name,superAdmin_password,channel,email_id,isDSCEnabled,mobile,superAdmin_role)
VALUES
(1,'adminmaker','7db06e9aee8f81962b228b0a6c035383b9ecf50a3e05a3380cff50782be52e82','super_admin',NULL,'false',NULL,'adminmaker');

 
INSERT INTO  public.super_admin_user
(superAdmin_users_id,superAdmin_name,superAdmin_password,channel,email_id,isDSCEnabled,mobile,superAdmin_role)
VALUES
(2,'adminchecker','4dd53d1f6f7fdc6bcdfed6fa94ec29a6350b4b208fd286514003f016b477a3ea','super_admin', NULL,'false', NULL,'adminchecker');
 
-------------------------------------------------------------------------------------------------------------------------

INSERT INTO  public.create_channel
(channel_id,channel_name)
VALUES
(1,'ch1'),
(2,'ch2'),
(3,'ch3'),
(4,'ch4'),
(5,'ch5');

-------------------------------------------------------------------------------------------------------------------------

--login super Admin

--userrName:   adminmaker
--Password:    adminmaker 

--useName:  adminchecker
--Password :  adminchecker




--*************User Password for maker checker

--Maker@123  :   825e7c356cd29e5ddc3f3d5bec3c3bc6e6de678baa81ca4e9b1c623459920f0f

--Checker@123 :   eab2cc7917618de3d90b31012a26972b7d540199a6e10a5bd313ec1b340a03cd

--****************

insert  into public.emas_admin_users(admin_users_id,action,admin_name,admin_password,admin_password_salt,admin_users_cur_no,channel_id,city,country,created_by,created_dt,dob,email_id,emp_id,first_login,is_active,isDSCEnabled,location,login_attempts,manageDSC,mobile,modified_by,modified_dt,password_reset,report_export,role,state,status) values 

(1,NULL,'maker1','613b448639186c69daf3fe34f67411f259eba03909321a584411382e309303e8','deb7f16db7deed84437e9ec2868364346b659a33a7361be8886a9b8861b01919',NULL,'ch1',NULL,NULL,'adminmaker','2020-09-27 21:11:14',NULL,'sandeepcs090@gmail.com','maker1','1','1','N/A',NULL,NULL,'1','9916097106','adminchecker','2020-09-27 21:20:40',NULL,NULL,'Maker',NULL,'LIVE'),

(2,NULL,'checker1','613b448639186c69daf3fe34f67411f259eba03909321a584411382e309303e8','7a5052b9fcb17f9f63191cdfa1fb63086f1e67182092845ae705eea032cacd46',NULL,'ch1',NULL,NULL,'adminmaker','2020-09-27 21:11:27',NULL,'sandeep.y2@emudhra.com','checker1','1','1','N/A',NULL,NULL,'1','9999999999','adminchecker','2020-09-27 21:19:53',NULL,NULL,'Checker',NULL,'LIVE');

-------------------------------------------------------------------------------------------------------------------------

-- Drop table

DROP TABLE public.emas_cust;

CREATE TABLE public.emas_cust (
	emas_cust_id int8 NOT NULL,
	iss_ca varchar(255) NULL,
	channel_id varchar(255) NULL,
	created_dt timestamp NULL,
	flex_one varchar(255) NULL,
	flex_two varchar(255) NULL,
	modify_dt timestamp NULL,
	unique_id varchar(255) NULL,
	user_disabled varchar(255) NULL,
	user_enabled varchar(255) NULL,
	user_locked varchar(255) NULL,
	remarks varchar(255) NULL,
	temp_disable_days varchar(255) NULL,
	temp_disable_end_time varchar(255) NULL,
	temp_disable_start_time varchar(255) NULL,
	userdisabled varchar(255) NULL DEFAULT NULL::character varying,
	CONSTRAINT emas_cust_pkey PRIMARY KEY (emas_cust_id)
);


ALTER TABLE public.emas_auth_dtls ADD CONSTRAINT fk75a39b2911405f32 FOREIGN KEY (emas_cust_id) REFERENCES emas_cust(emas_cust_id);
ALTER TABLE public.emas_auth_dtls ADD CONSTRAINT fk75a39b29b32ad1f3 FOREIGN KEY (emas_cust_id) REFERENCES emas_cust(emas_cust_id);

--Permissions to given to the Production DB user

 ALTER TABLE public.emas_cust OWNER TO dsc;
 GRANT ALL ON TABLE public.emas_cust TO dsc;
-------------------------------------------------------------------------------------------------------------------------

-- Drop table

DROP TABLE public.cust_admin;

CREATE TABLE public.cust_admin (
	cust_admin_id int4 NOT NULL,
	firstlogin varchar(255) NULL,
	accno varchar(255) NULL,
	acctype varchar(255) NULL,
	cert_serial_no varchar(255) NULL,
	channel_id varchar(255) NULL,
	constitution varchar(255) NULL,
	corpid varchar(255) NULL,
	corpname varchar(255) NULL,
	createdby varchar(255) NULL,
	created_dt timestamp NULL,
	curnum varchar(255) NULL,
	custid varchar(255) NULL,
	custname varchar(255) NULL,
	cust_password varchar(255) NULL,
	cust_password_salt varchar(255) NULL,
	custtype varchar(255) NULL,
	dateofbirth timestamp NULL,
	email_id varchar(255) NULL,
	isauthorise varchar(255) NULL,
	islocked varchar(255) NULL,
	"location" varchar(255) NULL,
	maker_action varchar(255) NULL,
	mobile_no varchar(255) NULL,
	modifiedby varchar(255) NULL,
	modified_dt timestamp NULL,
	otherreason varchar(255) NULL,
	reason varchar(255) NULL,
	registration_status varchar(255) NULL,
	remarks varchar(255) NULL,
	status varchar(255) NULL,
	temp_disable_days varchar(255) NULL,
	temp_disable_end_time timestamp NULL,
	temp_disable_start_time timestamp NULL,
	unique_id varchar(255) NULL,
	corp_name varchar(255) NULL DEFAULT NULL::character varying,
	CONSTRAINT cust_admin_pkey PRIMARY KEY (cust_admin_id),
	CONSTRAINT cust_admin_unique_id_key UNIQUE (unique_id)
);

-- Alter table script

ALTER TABLE public.emas_DS_dtls alter COLUMN x500_isspncpl_b64e type VARCHAR(5000);
ALTER TABLE public.emas_DS_dtls alter COLUMN x500_pncpl_b64e type VARCHAR(5000);
ALTER TABLE public.emas_ds_dtls_hist alter COLUMN x500_isspncpl_b64e TYPE VARCHAR(5000);
ALTER TABLE public.emas_ds_dtls_hist alter COLUMN x500_pncpl_b64e type VARCHAR(5000);
ALTER TABLE public.emas_txn_status alter COLUMN expiry_status_dtl type VARCHAR(5000);
ALTER TABLE public.emas_txn_status alter COLUMN signature_status_dtl type VARCHAR(5000);
ALTER TABLE public.emas_txn_status alter COLUMN trust_store_status_dtl type VARCHAR(5000);
ALTER TABLE public.emas_txn_status alter COLUMN ocsp_status_dtl type VARCHAR(5000);
ALTER TABLE public.emas_txn_status alter COLUMN crl_status_dtl type VARCHAR(5000);

