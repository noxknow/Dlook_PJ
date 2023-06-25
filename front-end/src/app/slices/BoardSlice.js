import { createSlice } from '@reduxjs/toolkit';
import instance from 'app/slices/Instance';

const initialState = {
  boardNo: 1,
  boardWriter: null,
};

export const getBoardDetail = (urlType, boardNo, navigate, setData) => {
  let url = '';
  if (urlType === 'detail') url = `/boards/detail/${boardNo}`;
  else return;

  instance
    .get(url)
    .then(({ data }) => {
      if (!data) {
        window.alert('잘못된 접근 입니다.');
        return navigate('/boards/list?page=0');
      }
      setData(data);
      setBoardWriter(data.boardWriter);
    })
    .catch((error) => console.log(error));
};

export const setBoardCtgLabel = (boardCtg) => {
  if (boardCtg === 'list') return '전체게시판';
  else if (boardCtg === 0) return '자유게시판';
  else return 'Q&A게시판';
};

const boardSlice = createSlice({
  name: 'board',
  initialState,
  reducers: {
    setBoardNo: (state, action) => {
      state.boardNo = action.payload;
    },
    setBoardWriter: (state, action) => {
      state.boardWriter = action.payload;
    },
  },
});

export const { setBoardNo, setBoardWriter } = boardSlice.actions;
export default boardSlice.reducer;
