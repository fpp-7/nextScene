import React, { useState } from 'react';
import { Image, View, StyleSheet, ImageStyle, ImageSourcePropType } from 'react-native';
import { Film } from 'lucide-react-native';
import { colors } from '../theme/colors';

interface Props {
  source: ImageSourcePropType;
  style?: ImageStyle;
}

export function ImageFallback({ source, style }: Props) {
  const [hasError, setHasError] = useState(false);

  if (hasError) {
    return (
      <View style={[styles.fallback, style]}>
        <Film size={24} color={colors.mutedForeground} />
      </View>
    );
  }

  return (
    <Image
      source={source}
      style={style}
      onError={() => setHasError(true)}
    />
  );
}

const styles = StyleSheet.create({
  fallback: {
    backgroundColor: colors.secondary,
    justifyContent: 'center',
    alignItems: 'center',
  },
});
