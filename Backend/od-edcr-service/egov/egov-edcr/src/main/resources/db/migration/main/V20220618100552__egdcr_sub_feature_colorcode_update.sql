delete from generic.egdcr_sub_feature_colorcode where feature = 'AffectedLandArea' and subfeature = 'CDP road widening';

update generic.egdcr_sub_feature_colorcode set subfeature = 'CDP proposed drain' where feature = 'AffectedLandArea' and subfeature = 'CDP drain widening';
