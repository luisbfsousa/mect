import { useContext } from 'react';
import { FlagsmithContext } from 'flagsmith/react';

const emptyResult = { isEnabled: false, value: null };

export const useFeatureFlag = (featureName, fallback = true) => {
  const context = useContext(FlagsmithContext);
  const client = context?.flagsmith || context;
  
  if (!client) {
    return { isEnabled: true, value: null };
  }

  if (!featureName) {
    return { ...emptyResult, isEnabled: fallback };
  }

  const isEnabled = client.hasFeature(featureName);
  const value = client.getValue(featureName);

  return {
    isEnabled: typeof isEnabled === 'boolean' ? isEnabled : fallback,
    value: value ?? null,
  };
};

export default useFeatureFlag;
