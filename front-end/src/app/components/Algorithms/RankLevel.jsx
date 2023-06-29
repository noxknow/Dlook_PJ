import { Level, StyleDiv, StyleSpan } from 'app/style/StyleAlgorithms';

const RankLevel = (level) => {
  const userLevel = level.level;
  return (
    <>
      <StyleDiv>
        <Level>
          <li></li>
          <li></li>
        </Level>
        <StyleSpan key={userLevel}>{userLevel}</StyleSpan>
      </StyleDiv>
    </>
  );
};

export default RankLevel;
